# Rate Limiting Strategy

## Overview

Multi-level rate limiting to protect against abuse: API endpoints, authentication, notifications, external APIs. Implemented via Redis + Spring filter chain.

---

## Rate Limiting Layers

```
Internet -> nginx (connection limit) -> Spring Filter (API rate limit) -> Business Logic (per-resource limits)
```

| Layer | Tool | Scope |
|-------|------|-------|
| L1: Connection | nginx `limit_req` | IP-based, coarse |
| L2: API | Redis token bucket | Per-user, fine-grained |
| L3: Business | In-process | Per-resource |
| L4: Outbound | Token bucket | Per-external-API |

---

## L1: nginx Connection Limiting

```nginx
limit_req_zone $binary_remote_addr zone=api:10m rate=30r/s;
limit_req_zone $binary_remote_addr zone=auth:1m rate=5r/m;

server {
    location /api/v1/auth/ {
        limit_req zone=auth burst=3 nodelay;
        limit_req_status 429;
    }

    location /api/v1/ {
        limit_req zone=api burst=50 nodelay;
        limit_req_status 429;
    }
}
```

---

## L2: API Rate Limiting (Redis Token Bucket)

### Rate Limit Tiers

| Endpoint Group | Rate | Burst | Window | Key |
|---------------|------|-------|--------|-----|
| Auth (`/auth/validate`) | 5/min | 3 | 60s | `rl:auth:{ip}` |
| Deals (write) | 20/min | 5 | 60s | `rl:deals:w:{userId}` |
| Deals (read) | 60/min | 20 | 60s | `rl:deals:r:{userId}` |
| Channels (search) | 30/min | 10 | 60s | `rl:channels:{userId}` |
| Creative (upload) | 10/min | 3 | 60s | `rl:creative:{userId}` |
| Dispute (submit) | 5/min | 2 | 60s | `rl:dispute:{userId}` |
| Admin endpoints | 120/min | 30 | 60s | `rl:admin:{userId}` |
| Global per-user | 120/min | 30 | 60s | `rl:global:{userId}` |

### Redis Token Bucket Algorithm

Lua script for atomic check + update:

```lua
-- KEYS[1] = rate limit key
-- ARGV[1] = max tokens (burst)
-- ARGV[2] = refill rate (tokens/sec)
-- ARGV[3] = current time (ms)
-- ARGV[4] = tokens to consume (1)
-- Returns: {allowed (0/1), remaining, retry_after_ms}

local key = KEYS[1]
local max_tokens = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local consume = tonumber(ARGV[4])

local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(bucket[1]) or max_tokens
local last_refill = tonumber(bucket[2]) or now

local elapsed = (now - last_refill) / 1000
local refill = elapsed * refill_rate
tokens = math.min(max_tokens, tokens + refill)

if tokens >= consume then
    tokens = tokens - consume
    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
    redis.call('EXPIRE', key, math.ceil(max_tokens / refill_rate) + 1)
    return {1, math.floor(tokens), 0}
else
    local retry_after = math.ceil((consume - tokens) / refill_rate * 1000)
    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
    redis.call('EXPIRE', key, math.ceil(max_tokens / refill_rate) + 1)
    return {0, 0, retry_after}
end
```

### Response Headers

```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1705315800
Retry-After: 30  (only on 429)
```

### 429 Response Body

```json
{
  "type": "https://api.admarketplace.com/errors/rate-limit-exceeded",
  "title": "Rate Limit Exceeded",
  "status": 429,
  "detail": "You have exceeded the rate limit of 60 requests per minute",
  "retryAfter": 30
}
```

---

## L3: Business-Level Limits

### Deal Creation

| Constraint | Limit | Rationale |
|-----------|-------|-----------|
| Active deals per advertiser | 10 concurrent | Prevent resource exhaustion |
| Deals per channel per day | 5 | Anti-spam |
| Disputes per user per week | 3 | Prevent abuse |
| Counter-offers per negotiation | 10 | Prevent infinite loop |

### Enforcement

It is checked in the service layer before the entity is created. Not through Redis - through DB COUNT query.

```java
long activeDealCount = dealRepo.countByAdvertiserAndStatusNotIn(
    userId, Set.of(CANCELLED, EXPIRED, COMPLETED_RELEASED, REFUNDED));
if (activeDealCount >= MAX_ACTIVE_DEALS) {
    throw new BusinessLimitExceededException("MAX_ACTIVE_DEALS", 10);
}
```

---

## L4: Outbound API Rate Limiting

### TON Center API

| Tier | Rate | Implementation |
|------|------|---------------|
| Basic key | 10 req/s | Guava RateLimiter (in-process) |
| Advanced key | 25 req/s | Guava RateLimiter (in-process) |

```java
private final RateLimiter tonApiLimiter = RateLimiter.create(10.0); // 10 req/s

public TonResponse callTonApi(TonRequest request) {
    tonApiLimiter.acquire(); // blocks until permit available
    return tonClient.execute(request);
}
```

### Telegram Bot API

| Limit | Rate | Key |
|-------|------|-----|
| Different chats | 30 msg/s | Global semaphore |
| Same chat | 1 msg/s | Per-chatId RateLimiter |
| Same group | 20 msg/min | Per-groupId RateLimiter |

The implementation is described in [02-telegram-bot-framework.md](./02-telegram-bot-framework.md).

---

## Auth Brute-Force Protection

### Progressive Delays

| Failed Attempts | Action |
|----------------|--------|
| 1-3 | Normal response |
| 4-5 | +2s delay before response |
| 6-10 | +5s delay, CAPTCHA warning in response |
| 11+ | Account locked for 15 min, notify user via bot |

### Tracking

```
Redis key: rl:auth:fail:{telegramUserId}
TTL: 15 minutes (sliding window)
Value: failed attempt count
```

### IP Blacklist (emergency)

```
Redis SET: rl:blacklist:ip
```

For DDoS - nginx `deny` + Redis SET check in Spring filter.

---

## Monitoring

| Metric | Type | Tags |
|--------|------|------|
| `ratelimit.requests.total` | Counter | tier, endpoint |
| `ratelimit.requests.limited` | Counter | tier, endpoint |
| `ratelimit.auth.failures` | Counter | -- |
| `ratelimit.auth.lockouts` | Counter | -- |

### Alerts

| Alert | Condition | Severity |
|-------|-----------|----------|
| Auth brute-force | `ratelimit.auth.failures` rate > 50/min | WARNING |
| Auth lockouts spike | `ratelimit.auth.lockouts` rate > 10/min | CRITICAL |
| API rate limiting > 5% | `limited / total > 0.05` for 5 min | WARNING |

---

## Configuration

```yaml
rate-limit:
  enabled: true
  redis-key-prefix: rl
  tiers:
    auth:
      rate: 5
      burst: 3
      window: 60s
    api-write:
      rate: 20
      burst: 5
      window: 60s
    api-read:
      rate: 60
      burst: 20
      window: 60s
  business:
    max-active-deals-per-advertiser: 10
    max-deals-per-channel-per-day: 5
    max-disputes-per-user-per-week: 3
  outbound:
    ton-api-rate: 10.0
```

---

## Related Documents

- [Auth Flow](./03-auth-flow.md)
- [Telegram Bot Framework](./02-telegram-bot-framework.md)
- [TON SDK Integration](./01-ton-sdk-integration.md)
- [Deployment Runbook](./23-deployment-runbook.md)
- [Metrics & SLOs](./21-metrics-slos-monitoring.md)
