# Deal Timeout Scheduler Implementation

## Overview

Deals have time-limited states. When deadlines expire, the system automatically transitions deals (expire, cancel, refund).

---

## Scheduler Technology

**Decision**: Spring `@Scheduled` + database polling + Kafka `deal.deadlines` topic.

| Approach | Pros | Cons |
|----------|------|------|
| Spring @Scheduled + DB poll | Simple, reliable, no extra infra | Polling overhead |
| Kafka delayed messages | Natural event flow | Kafka has no native delay |
| External scheduler (Quartz) | Rich scheduling | Extra dependency |

**Chosen**: DB polling via Spring `@Scheduled` (simplest for MVP).

---

## Deadline Storage

Stored in `deals.deadline_at` column:

```sql
-- Set deadline when deal enters time-limited state
UPDATE deals
SET deadline_at = now() + INTERVAL ':timeout',
    updated_at = now()
WHERE id = :deal_id;
```

---

## Timeout Values per State

| State | Timeout | Action on Expiry |
|-------|---------|-----------------|
| OFFER_PENDING | 48h | EXPIRE (no refund needed) |
| NEGOTIATING | 72h | EXPIRE (no refund needed) |
| AWAITING_PAYMENT | 24h | EXPIRE (no refund needed) |
| FUNDED | 72h | EXPIRE + REFUND |
| CREATIVE_SUBMITTED | 48h | EXPIRE + REFUND |
| CREATIVE_APPROVED | 48h | EXPIRE + REFUND |
| SCHEDULED | 24h | EXPIRE + REFUND |
| DELIVERY_VERIFYING | 24h | COMPLETED_RELEASED (if verified) / DISPUTED (if API error) |
| DISPUTED | 7 days | Escalate to operator |

### Configuration

```yaml
deal:
  timeouts:
    offer-pending: 48h
    negotiating: 72h
    awaiting-payment: 24h
    funded: 72h
    creative-submitted: 48h
    creative-approved: 48h
    scheduled: 24h
    delivery-verifying: 24h
    disputed: 168h  # 7 days
  scheduler:
    poll-interval: 30s
    batch-size: 100
```

---

## Polling Query

```sql
SELECT id, status, deadline_at
FROM deals
WHERE deadline_at <= now()
  AND status NOT IN ('COMPLETED_RELEASED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'EXPIRED')
ORDER BY deadline_at ASC
LIMIT :batchSize
FOR UPDATE SKIP LOCKED;
```

---

## Processing Flow

1. Scheduler polls every 30 seconds
2. For each expired deal:
   a. Acquire distributed lock `lock:deal:{deal_id}`
   b. Verify deal still in expected state (re-read)
   c. Determine action based on current state
   d. Execute state transition
   e. If refund required: emit `EXECUTE_REFUND` to escrow.commands
   f. Emit `DEAL_STATE_CHANGED` event to deal.events
   g. Send notifications to participants
   h. Clear `deadline_at`
   i. Release lock

### Kafka deal.deadlines Topic

Used for explicit deadline events (alternative to polling):

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

## Grace Period

- 5-minute grace period after deadline before executing action
- Allows for in-flight transactions to complete
- Prevents race conditions with user actions near deadline

```sql
WHERE deadline_at <= now() - INTERVAL '5 minutes'
```

---

## Deadline Reset

When user takes action before deadline:
1. Clear `deadline_at` for current state
2. Set new `deadline_at` for next state (if applicable)
3. State machine transition handles this automatically

---

## Idempotency

- Timeout processing uses distributed lock (prevents double processing)
- Re-read deal state after acquiring lock (prevents stale state processing)
- Idempotency key for refunds: `timeout-refund:{deal_id}`

---

## Related Documents

- [Deal State Machine](../06-deal-state-machine.md)
- [Deal Lifecycle Feature](../03-feature-specs/02-deal-lifecycle.md)
- [Redis Distributed Locks](./09-redis-distributed-locks.md)