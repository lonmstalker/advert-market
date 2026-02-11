# Redis Distributed Lock Implementation

## Redis Client: Lettuce

**Decision**: Lettuce (Spring Boot default) -- non-blocking, thread-safe, connection pooling.

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    // Lettuce is the default client
}
```

---

## Lock Primitive: SET NX EX

### Acquire Lock

```
SET lock:{resource} {fencing_token} NX EX {ttl_seconds}
```

- `NX` -- only set if not exists
- `EX` -- expiration in seconds
- `fencing_token` -- UUID for safe release

### Release Lock (Lua Script)

```lua
-- release_lock.lua
if redis.call("GET", KEYS[1]) == ARGV[1] then
    return redis.call("DEL", KEYS[1])
else
    return 0
end
```

Only the holder (matching fencing token) can release the lock.

---

## Lock Key Schema

| Lock Purpose | Key Pattern | TTL |
|-------------|-------------|-----|
| Deal state transition | `lock:deal:{deal_id}` | 30s |
| Payout execution | `lock:payout:{deal_id}` | 60s |
| Refund execution | `lock:refund:{deal_id}` | 60s |
| Deposit processing | `lock:deposit:{deal_id}` | 30s |
| Balance update | `lock:balance:{account_id}` | 10s |
| Outbox batch | `lock:outbox:poller` | 30s |
| Reconciliation | `lock:reconciliation` | 300s |

---

## Fencing Token

- Generated as `UUID.randomUUID().toString()` on acquire
- Stored as lock value in Redis
- Passed to release script to prevent accidental release by another holder
- For financial operations: include fencing token in downstream commands

---

## Lock Acquisition Strategy

| Parameter | Value |
|-----------|-------|
| Max wait time | 5 seconds |
| Retry interval | 50ms |
| Max retries | 100 (= 5s / 50ms) |
| Spin strategy | Thread.sleep(50) between retries |

### Failure Handling

- If lock not acquired within timeout: throw `LockAcquisitionException`
- Deal state transitions: return 409 Conflict to client
- Financial operations: requeue Kafka message for retry
- Never proceed without lock for financial operations

---

## Stale Lock Cleanup

Redis TTL handles stale locks automatically:
- Lock expires after TTL even if holder crashes
- TTL chosen to be longer than expected operation time
- No manual cleanup needed

### Extension (Watchdog)

For long-running operations (reconciliation):
- Background thread extends lock TTL every `TTL/3` seconds
- Stops extending on operation completion
- If operation dies, lock naturally expires after TTL

---

## Idempotency Keys

Separate from locks, used to prevent duplicate processing:

```
Key: idempotency:{operation}:{unique_key}
Value: "1"
TTL: 24h
```

| Operation | Key Format |
|-----------|-----------|
| Deposit confirmation | `idempotency:deposit:{tx_hash}` |
| Payout execution | `idempotency:payout:{deal_id}` |
| Refund execution | `idempotency:refund:{deal_id}` |
| Notification send | `idempotency:notify:{outbox_id}` |

### Check-and-Set Pattern

1. `SET idempotency:{key} 1 NX EX 86400`
2. If SET returns OK: proceed with operation
3. If SET returns nil: operation already processed, skip

---

## Scaled Deployment: Redlock

For multi-Redis-node setup:
- Use Redlock algorithm (acquire lock on N/2+1 nodes)
- Library: `org.redisson:redisson` with `RLock`
- Only needed when Redis is clustered (Scaled deployment)

MVP uses single Redis instance -- simple SET NX EX is sufficient.

---

## Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2s
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5

lock:
  default-ttl: 30s
  acquisition-timeout: 5s
  retry-interval: 50ms
```

---

## Related Documents

- [Redis Usage](../04-architecture/07-redis-usage.md)
- [Idempotency Strategy](../05-patterns-and-decisions/07-idempotency-strategy.md)
- [Escrow Flow](../07-financial-system/02-escrow-flow.md)