# Outbox Poller Implementation

## Overview

Transactional outbox pattern: application writes events to `notification_outbox` table within the same DB transaction as business logic. A separate poller reads pending records and publishes to Kafka.

---

## Polling Query

```sql
SELECT id, deal_id, recipient_id, template, payload, retry_count
FROM notification_outbox
WHERE status = 'PENDING'
  AND created_at < now() - INTERVAL '1 second'
ORDER BY created_at ASC
LIMIT :batchSize
FOR UPDATE SKIP LOCKED;
```

- `FOR UPDATE SKIP LOCKED` -- allows multiple poller instances without contention
- `1 second` delay -- avoids reading records from in-flight transactions
- `ORDER BY created_at ASC` -- FIFO ordering

---

## Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| Poll interval | 500ms | How often to check for new records |
| Batch size | 50 | Max records per poll |
| Max retries | 3 | Before marking FAILED |
| Retry backoff | 1s, 2s, 4s | Exponential |

```yaml
outbox:
  poller:
    interval: 500ms
    batch-size: 50
    max-retries: 3
    initial-backoff: 1s
```

---

## Record Lifecycle

```
PENDING -> PROCESSING -> DELIVERED
                      -> FAILED (after max retries)
```

### State Transitions

1. **PENDING -> PROCESSING**: Poller acquires record via `FOR UPDATE SKIP LOCKED`
2. **PROCESSING -> DELIVERED**: Kafka produce succeeds, update status + `processed_at`
3. **PROCESSING -> PENDING**: Kafka produce fails, increment `retry_count`, release lock
4. **PENDING -> FAILED**: `retry_count >= max_retries`, mark FAILED

### Update on Success

```sql
UPDATE notification_outbox
SET status = 'DELIVERED', processed_at = now()
WHERE id = :id;
```

### Update on Failure

```sql
UPDATE notification_outbox
SET status = CASE WHEN retry_count + 1 >= :maxRetries THEN 'FAILED' ELSE 'PENDING' END,
    retry_count = retry_count + 1
WHERE id = :id;
```

---

## Kafka Produce Error Handling

| Error Type | Action |
|------------|--------|
| Serialization error | Mark FAILED immediately (no retry) |
| Broker unavailable | Retry with backoff |
| Topic not found | Mark FAILED, alert |
| Timeout | Retry with backoff |
| Unknown error | Retry with backoff |

### Produce with Callback

1. Send message via `KafkaTemplate.send(topic, key, value)`
2. Use `CompletableFuture` from `send()` return
3. On success: mark DELIVERED
4. On failure: increment retry, log error

---

## Cleanup / Archival

### Delivered Records

```sql
-- Daily cleanup: delete delivered records older than 7 days
DELETE FROM notification_outbox
WHERE status = 'DELIVERED'
  AND processed_at < now() - INTERVAL '7 days';
```

### Failed Records

- Failed records kept for manual investigation
- Alert on `count(status='FAILED') > 0` in monitoring
- Manual reprocessing: reset `status = 'PENDING'`, `retry_count = 0`

---

## Monitoring Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `outbox.poll.count` | Counter | Number of poll cycles |
| `outbox.records.processed` | Counter | Records successfully delivered |
| `outbox.records.failed` | Counter | Records marked FAILED |
| `outbox.poll.duration` | Timer | Time per poll cycle |
| `outbox.lag` | Gauge | Count of PENDING records |

### Alerting

- `outbox.lag > 1000` for 5 minutes -> WARNING
- `outbox.lag > 5000` for 5 minutes -> CRITICAL
- `outbox.records.failed` rate > 10/min -> WARNING

---

## Distributed Lock for Poller

Optional: if multiple poller instances run, use Redis lock `lock:outbox:poller` (TTL 30s) to ensure only one polls at a time. With `SKIP LOCKED`, multiple pollers can safely run without lock but may cause unnecessary DB load.

---

## Related Documents

- [Transactional Outbox Pattern](../05-patterns-and-decisions/03-transactional-outbox.md)
- [Kafka Event Schemas](./04-kafka-event-schemas.md)
- [Notifications Feature](../03-feature-specs/08-notifications.md)
