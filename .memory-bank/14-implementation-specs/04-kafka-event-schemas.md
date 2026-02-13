# Kafka Event Schemas for All 8 Topics

## Serialization Format

**Decision**: JSON Schema for MVP, migrate to Avro for Scaled deployment.

| Format | MVP | Scaled |
|--------|-----|--------|
| JSON Schema | Simple, human-readable, no codegen | |
| Avro | | Compact, schema evolution, Schema Registry |

**Schema evolution**: BACKWARD compatibility -- consumers with new schema can read old messages.

---

## Common Event Envelope

All messages on all topics share this envelope:

```json
{
  "event_id": "UUID",
  "event_type": "string",
  "deal_id": "UUID (nullable for reconciliation)",
  "timestamp": "ISO 8601",
  "version": 1,
  "correlation_id": "UUID",
  "payload": { }
}
```

Partition key: `deal_id` (or `recipient_id` for notifications.outbox).

---

## Topic Schemas

### 1. deal.events

Domain events from deal state machine. Fan-out for various consumers.

**Event types**: All state transitions (`OFFER_SUBMITTED`, `OFFER_ACCEPTED`, `FUNDED`, `CREATIVE_SUBMITTED`, `CREATIVE_APPROVED`, `PUBLISHED`, `DELIVERY_VERIFIED`, `DISPUTED`, `CANCELLED`, `EXPIRED`, `COMPLETED`, `REFUNDED`)

```json
{
  "event_id": "...",
  "event_type": "DEAL_STATE_CHANGED",
  "deal_id": "...",
  "timestamp": "...",
  "version": 1,
  "payload": {
    "from_status": "AWAITING_PAYMENT",
    "to_status": "FUNDED",
    "actor_id": null,
    "actor_type": "SYSTEM",
    "deal_amount_nano": 1000000000,
    "channel_id": 123456
  }
}
```

### 2. escrow.commands

Commands for financial workers. Three command types, routed by `event_type`.

#### WATCH_DEPOSIT
```json
{
  "event_type": "WATCH_DEPOSIT",
  "deal_id": "...",
  "payload": {
    "deposit_address": "UQ...",
    "expected_amount_nano": 1000000000,
    "advertiser_id": 42
  }
}
```

#### EXECUTE_PAYOUT
```json
{
  "event_type": "EXECUTE_PAYOUT",
  "deal_id": "...",
  "payload": {
    "owner_id": 99,
    "amount_nano": 900000000,
    "commission_nano": 100000000,
    "subwallet_id": 12345
  }
}
```

#### EXECUTE_REFUND
```json
{
  "event_type": "EXECUTE_REFUND",
  "deal_id": "...",
  "payload": {
    "advertiser_id": 42,
    "amount_nano": 1000000000,
    "refund_address": "EQ...",
    "subwallet_id": 12345
  }
}
```

### 3. escrow.confirmations

Results from TON Deposit Watcher.

```json
{
  "event_type": "DEPOSIT_CONFIRMED",
  "deal_id": "...",
  "payload": {
    "tx_hash": "abc123...",
    "amount_nano": 1000000000,
    "confirmations": 3,
    "from_address": "EQ...",
    "deposit_address": "UQ..."
  }
}
```

Failure:
```json
{
  "event_type": "DEPOSIT_FAILED",
  "deal_id": "...",
  "payload": {
    "reason": "AMOUNT_MISMATCH",
    "expected_nano": 1000000000,
    "received_nano": 500000000
  }
}
```

### 4. delivery.commands

#### PUBLISH_POST
```json
{
  "event_type": "PUBLISH_POST",
  "deal_id": "...",
  "payload": {
    "channel_id": 123456,
    "creative_draft": { "text": "...", "media": [], "buttons": [] },
    "scheduled_at": "2025-01-15T14:00:00Z"
  }
}
```

#### VERIFY_DELIVERY
```json
{
  "event_type": "VERIFY_DELIVERY",
  "deal_id": "...",
  "payload": {
    "channel_id": 123456,
    "message_id": 789,
    "content_hash": "sha256:abc...",
    "published_at": "2025-01-15T14:00:00Z",
    "check_number": 1
  }
}
```

### 5. delivery.results

```json
{
  "event_type": "DELIVERY_VERIFIED",
  "deal_id": "...",
  "payload": {
    "message_id": 789,
    "checks_passed": 5,
    "checks_failed": 0,
    "final_content_hash": "sha256:abc..."
  }
}
```

Failure:
```json
{
  "event_type": "DELIVERY_FAILED",
  "deal_id": "...",
  "payload": {
    "message_id": 789,
    "reason": "POST_DELETED",
    "check_number": 3,
    "detected_at": "2025-01-15T20:00:00Z"
  }
}
```

### 6. notifications.outbox

Partition key: `recipient_id` (not deal_id).

```json
{
  "event_type": "NOTIFICATION",
  "deal_id": "...",
  "payload": {
    "recipient_id": 42,
    "template": "ESCROW_FUNDED",
    "locale": "ru",
    "vars": {
      "channel_name": "Tech Channel",
      "amount": "100",
      "deal_id_short": "550e8400"
    },
    "buttons": [
      {"text": "Open deal", "url": "https://t.me/bot?startapp=deal_550e8400"}
    ]
  }
}
```

#### SWEEP_COMMISSION
```json
{
  "event_type": "SWEEP_COMMISSION",
  "deal_id": "...",
  "payload": {
    "commission_account_id": "COMMISSION:deal-123",
    "amount_nano": 100000000,
    "subwallet_id": 12345
  }
}
```

#### AUTO_REFUND_LATE_DEPOSIT
```json
{
  "event_type": "AUTO_REFUND_LATE_DEPOSIT",
  "deal_id": "...",
  "payload": {
    "tx_hash": "abc123...",
    "amount_nano": 1000000000,
    "refund_address": "EQ...",
    "subwallet_id": 12345
  }
}
```

### 7. reconciliation.triggers

```json
{
  "event_type": "RECONCILIATION_START",
  "deal_id": null,
  "payload": {
    "trigger_type": "SCHEDULED",
    "time_range_start": "2025-01-14T00:00:00Z",
    "time_range_end": "2025-01-15T00:00:00Z",
    "checks": ["LEDGER_BALANCE", "LEDGER_VS_TON", "LEDGER_VS_DEALS", "CQRS_PROJECTION"]
  }
}
```

### 8. deal.deadlines

```json
{
  "event_type": "DEADLINE_SET",
  "deal_id": "...",
  "payload": {
    "expected_status": "AWAITING_PAYMENT",
    "deadline_at": "2025-01-16T10:30:00Z",
    "action": "EXPIRE",
    "refund_required": false
  }
}
```

---

## Consumer Error Handling

### Dead Letter Topics

Convention: `{topic}.DLT` (e.g., `escrow.commands.DLT`)

DLT message envelope:
```json
{
  "original_topic": "escrow.commands",
  "original_partition": 0,
  "original_offset": 12345,
  "error_message": "NullPointerException: ...",
  "consumer_group": "cg-payout-executor",
  "failed_at": "2025-01-15T10:30:00Z",
  "retry_count": 3,
  "original_message": { }
}
```

### Deserialization Errors

- JSON parse failure -> immediately to DLT (no retry)
- Missing required field -> immediately to DLT
- Business logic error -> retry with backoff, then DLT

---

## Schema Evolution Rules

1. **Adding optional fields**: allowed (BACKWARD compatible)
2. **Removing fields**: consumer must tolerate missing fields
3. **Changing field types**: NOT allowed
4. **Renaming fields**: NOT allowed (add new + deprecate old)
5. **Version field**: increment `version` on breaking changes

---

## Spring Kafka Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        max.poll.records: 100
```

---

## Related Documents

- [Kafka Topology](../04-architecture/06-kafka-topology.md)
- [Worker Callback Schemas](./10-worker-callback-schemas.md)
- [Transactional Outbox](../05-patterns-and-decisions/03-transactional-outbox.md)