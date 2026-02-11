
# Worker Callback Payload Schemas (All 6 Types)

## Overview

Workers communicate results back to Backend API via **Kafka topics** (not HTTP callbacks). Each result type has a defined schema, idempotency key, and retry policy.

---

## 1. DEPOSIT_CONFIRMED

**Topic**: `escrow.confirmations`
**Consumer group**: `cg-deposit-handler`

```json
{
  "event_type": "DEPOSIT_CONFIRMED",
  "deal_id": "550e8400-e29b-41d4-a716-446655440000",
  "payload": {
    "tx_hash": "abc123def456...",
    "amount_nano": 1000000000,
    "confirmations": 3,
    "from_address": "EQabc...",
    "deposit_address": "UQxyz..."
  }
}
```

**Idempotency key**: `deposit:{tx_hash}`
**Action**: Transition deal AWAITING_PAYMENT -> FUNDED, create ledger entries

---

## 2. PAYOUT_COMPLETED

**Topic**: `escrow.confirmations`
**Consumer group**: `cg-deposit-handler`

```json
{
  "event_type": "PAYOUT_COMPLETED",
  "deal_id": "...",
  "payload": {
    "tx_hash": "def789...",
    "amount_nano": 900000000,
    "commission_nano": 100000000,
    "to_address": "EQowner...",
    "confirmations": 3
  }
}
```

**Idempotency key**: `payout:{deal_id}`
**Action**: Transition deal -> COMPLETED, create payout ledger entries

---

## 3. REFUND_COMPLETED

**Topic**: `escrow.confirmations`
**Consumer group**: `cg-deposit-handler`

```json
{
  "event_type": "REFUND_COMPLETED",
  "deal_id": "...",
  "payload": {
    "tx_hash": "ghi012...",
    "amount_nano": 1000000000,
    "to_address": "EQadvertiser...",
    "confirmations": 3
  }
}
```

**Idempotency key**: `refund:{deal_id}`
**Action**: Transition deal -> REFUNDED, create refund ledger entries

---

## 4. PUBLICATION_RESULT

**Topic**: `delivery.results`
**Consumer group**: `cg-delivery-handler`

```json
{
  "event_type": "PUBLICATION_RESULT",
  "deal_id": "...",
  "payload": {
    "success": true,
    "message_id": 789,
    "channel_id": 123456,
    "content_hash": "sha256:abc...",
    "published_at": "2025-01-15T14:00:00Z"
  }
}
```

**Idempotency key**: `publish:{deal_id}`
**Action**: Transition deal -> PUBLISHED, store message_id and content_hash

On failure:
```json
{
  "event_type": "PUBLICATION_RESULT",
  "deal_id": "...",
  "payload": {
    "success": false,
    "error": "CHANNEL_NOT_ACCESSIBLE",
    "details": "Bot removed from channel"
  }
}
```

---

## 5. VERIFICATION_RESULT

**Topic**: `delivery.results`
**Consumer group**: `cg-delivery-handler`

```json
{
  "event_type": "VERIFICATION_RESULT",
  "deal_id": "...",
  "payload": {
    "verified": true,
    "message_id": 789,
    "checks_passed": 5,
    "checks_failed": 0,
    "final_content_hash": "sha256:abc..."
  }
}
```

**Idempotency key**: `verify:{deal_id}:{check_number}`
**Action**: If final check passed -> transition PUBLISHED -> DELIVERY_VERIFIED

On failure:
```json
{
  "event_type": "VERIFICATION_RESULT",
  "deal_id": "...",
  "payload": {
    "verified": false,
    "reason": "POST_DELETED",
    "check_number": 3,
    "detected_at": "2025-01-15T20:00:00Z"
  }
}
```

**Action**: Auto-open dispute with reason `POST_DELETED`

---

## 6. RECONCILIATION_RESULT

**Topic**: `reconciliation.triggers` (response uses same topic or dedicated results topic)
**Consumer group**: `cg-reconciliation`

```json
{
  "event_type": "RECONCILIATION_RESULT",
  "deal_id": null,
  "payload": {
    "trigger_id": "uuid...",
    "checks": {
      "LEDGER_BALANCE": {"status": "PASS", "details": {}},
      "LEDGER_VS_TON": {"status": "FAIL", "details": {"diff_nano": 5000000}},
      "LEDGER_VS_DEALS": {"status": "PASS", "details": {}},
      "CQRS_PROJECTION": {"status": "PASS", "details": {}}
    },
    "completed_at": "2025-01-15T01:00:00Z"
  }
}
```

**Idempotency key**: `recon:{trigger_id}`
**Action**: Store results, send RECONCILIATION_ALERT notification if any check FAIL

---

## Retry Policy

| Worker | Max Retries | Backoff | Timeout |
|--------|-------------|---------|---------|
| TON Deposit Watcher | 5 | 2s, 4s, 8s, 16s, 32s | 60s |
| Payout Executor | 3 | 5s, 10s, 20s | 120s |
| Refund Executor | 3 | 5s, 10s, 20s | 120s |
| Auto-Publisher | 3 | 2s, 4s, 8s | 30s |
| Delivery Verifier | 5 | 60s, 120s, 240s, 480s, 960s | 30s |
| Reconciliation | 1 | -- | 300s |

After max retries: send to DLT, create alert.

---

## Security: Worker-to-Backend Communication

### Authentication Model

Workers и Backend API находятся в одном Docker Compose (MVP). Коммуникация идёт через Kafka topics, а НЕ через HTTP.

| Layer | Mechanism |
|-------|-----------|
| Network | Docker internal network (workers не доступны извне) |
| Kafka | SASL/PLAIN auth (когда Kafka развёрнут с аутентификацией) |
| Message-level | HMAC signature в Kafka header |

### HMAC Message Signing

Каждое сообщение от worker содержит HMAC-SHA256 подпись в Kafka header:

```
Header: X-Worker-Signature = HMAC-SHA256(shared_secret, message_body)
Header: X-Worker-Id = worker-name
Header: X-Worker-Timestamp = epoch_ms
```

Consumer верифицирует:
1. `X-Worker-Timestamp` не старше 5 минут (replay protection)
2. HMAC совпадает с пересчитанным из `shared_secret + body`
3. `X-Worker-Id` в allow-list

### Internal HTTP API (если используется)

Для `POST /internal/v1/worker-events` (fallback):

| Mechanism | Details |
|-----------|---------|
| Network policy | Доступен только из Docker internal network |
| API key | `X-Internal-Api-Key` header (env var `INTERNAL_API_KEY`) |
| IP whitelist | Только 172.18.0.0/16 (Docker bridge) |

```yaml
internal:
  api:
    key: ${INTERNAL_API_KEY}
    allowed-networks:
      - 172.18.0.0/16
      - 127.0.0.1/32
```

### Production Path (Scaled Deployment)

При переходе на Kubernetes:
- mTLS между сервисами (Istio/Linkerd)
- Kafka с SASL/SCRAM + TLS
- Network policies (Kubernetes NetworkPolicy)

---

## Enhanced Idempotency

### Idempotency Key Storage

```sql
CREATE TABLE processed_events (
    idempotency_key VARCHAR(200) PRIMARY KEY,
    event_type      VARCHAR(50) NOT NULL,
    deal_id         UUID,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    result          VARCHAR(20) NOT NULL  -- SUCCESS, SKIPPED, FAILED
);

-- Cleanup: remove records older than 30 days
-- Handled by scheduled task
```

### Processing Algorithm

```java
@Transactional
public void processWorkerEvent(WorkerEvent event) {
    String key = event.idempotencyKey();

    // 1. Check if already processed
    if (processedEventRepo.existsByKey(key)) {
        log.info("Duplicate event ignored: {}", key);
        metrics.counter("worker.events.duplicate").increment();
        return;
    }

    // 2. Process event (state transition, ledger entries, etc.)
    processEvent(event);

    // 3. Record as processed (within same transaction)
    processedEventRepo.insert(key, event.eventType(), event.dealId(), "SUCCESS");
}
```

### Idempotency Key Formats (Summary)

| Event Type | Key Format | Uniqueness |
|-----------|-----------|------------|
| DEPOSIT_CONFIRMED | `deposit:{tx_hash}` | Per blockchain TX |
| PAYOUT_COMPLETED | `payout:{deal_id}` | Per deal (only 1 payout) |
| REFUND_COMPLETED | `refund:{deal_id}` | Per deal (only 1 refund) |
| PUBLICATION_RESULT | `publish:{deal_id}` | Per deal |
| VERIFICATION_RESULT | `verify:{deal_id}:{check_number}` | Per verification check |
| RECONCILIATION_RESULT | `recon:{trigger_id}` | Per reconciliation run |

---

## Related Documents

- [Kafka Event Schemas](./04-kafka-event-schemas.md)
- [Workers Architecture](../04-architecture/04-workers.md)
- [Kafka Consumer Error Handling](./18-kafka-consumer-error-handling.md)