# Kafka Consumer Error Handling Strategy

## Overview

Spring Kafka consumer error handling with retry, dead letter topics, and poison message isolation.

---

## Consumer Groups

| Consumer Group | Topic | Purpose |
|---------------|-------|---------|
| `cg-deal-events` | deal.events | Fan-out: notifications, deadline scheduling |
| `cg-deposit-watcher` | escrow.commands | TON Deposit Watcher |
| `cg-payout-executor` | escrow.commands | Payout/Refund Executor |
| `cg-deposit-handler` | escrow.confirmations | Process deposit/payout/refund confirmations |
| `cg-auto-publisher` | delivery.commands | Auto-Publisher |
| `cg-delivery-verifier` | delivery.commands | Delivery Verifier |
| `cg-delivery-handler` | delivery.results | Process delivery results |
| `cg-notification-sender` | notifications.outbox | Telegram message sender |
| `cg-deadline-handler` | deal.deadlines | Deal Timeout Worker |
| `cg-reconciliation` | reconciliation.triggers | Reconciliation Worker |

---

## Retry Policy

### Default Retry (Spring Kafka DefaultErrorHandler)

```yaml
spring:
  kafka:
    listener:
      ack-mode: MANUAL_IMMEDIATE
```

| Parameter | Default | Financial Topics | Notification Topics |
|-----------|---------|-----------------|-------------------|
| Max retries | 3 | 5 | 3 |
| Initial backoff | 1s | 5s | 1s |
| Max backoff | 30s | 60s | 10s |
| Multiplier | 2 | 2 | 2 |

### Per-Consumer Configuration

- Financial consumers (escrow.commands, escrow.confirmations): longer backoff, more retries
- Notification consumers: shorter backoff, fast failure
- Reconciliation: no retry (idempotent, re-triggered by schedule)

---

## Dead Letter Topics (DLT)

### Naming Convention

`{original_topic}.DLT`

| Source Topic | DLT |
|-------------|-----|
| deal.events | deal.events.DLT |
| escrow.commands | escrow.commands.DLT |
| escrow.confirmations | escrow.confirmations.DLT |
| delivery.commands | delivery.commands.DLT |
| delivery.results | delivery.results.DLT |
| notifications.outbox | notifications.outbox.DLT |
| deal.deadlines | deal.deadlines.DLT |
| reconciliation.triggers | reconciliation.triggers.DLT |

### DLT Message Headers

Spring Kafka automatically adds:
- `kafka_dlt-exception-fqcn` -- exception class name
- `kafka_dlt-exception-message` -- error message
- `kafka_dlt-exception-stacktrace` -- truncated stack trace
- `kafka_dlt-original-topic` -- source topic
- `kafka_dlt-original-partition` -- source partition
- `kafka_dlt-original-offset` -- source offset
- `kafka_dlt-original-timestamp` -- original message timestamp

---

## Poison Message Handling

### Detection

A message is "poison" if it causes the same exception on every retry attempt:
- Deserialization failure (malformed JSON)
- Missing required field
- Invalid enum value
- Schema version mismatch

### Isolation Strategy

1. **Deserialization errors**: Immediately to DLT (no retry)
   - Use `ErrorHandlingDeserializer` wrapper
   - Invalid JSON -> DLT without consumer seeing the message

2. **Validation errors**: Immediately to DLT (no retry)
   - Missing required fields
   - Invalid field values

3. **Business logic errors**: Retry with backoff, then DLT
   - Transient DB errors
   - Lock contention
   - External API failures

---

## Offset Commit Strategy

**Manual commit after processing** (`ack-mode: MANUAL_IMMEDIATE`):

1. Consumer receives message
2. Process message
3. On success: `acknowledgment.acknowledge()` -- commits offset
4. On failure: exception bubbles to error handler
5. Error handler retries or sends to DLT
6. After DLT: offset committed (message won't be re-delivered)

### Why Manual Commit?

- Prevents message loss on consumer crash
- Ensures at-least-once delivery
- Combined with idempotency keys for exactly-once semantics

---

## Consumer Lag Alerting

| Metric | Warning | Critical |
|--------|---------|----------|
| `kafka.consumer.lag` (financial topics) | > 100 | > 1000 |
| `kafka.consumer.lag` (notification topics) | > 500 | > 5000 |
| `kafka.consumer.lag` (other topics) | > 1000 | > 10000 |
| DLT message count (any topic) | > 0 | > 10 |

### Monitoring

```
kafka_consumer_records_lag_max{group="cg-payout-executor"}
kafka_consumer_records_consumed_total{group="cg-deposit-handler"}
```

---

## Rebalancing Strategy

- `partition.assignment.strategy`: CooperativeSticky
- Avoids stop-the-world rebalances
- Sticky assignment minimizes partition movement

```yaml
spring:
  kafka:
    consumer:
      properties:
        partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

---

## DLT Manual Reprocessing

For operators to reprocess failed messages:

1. Read from DLT topic
2. Fix the underlying issue (code fix, data fix)
3. Republish message to original topic
4. Or mark as permanently failed in audit log

---

## Related Documents

- [Kafka Event Schemas](./04-kafka-event-schemas.md)
- [Kafka Topology](../04-architecture/06-kafka-topology.md)
- [Worker Callback Schemas](./10-worker-callback-schemas.md)