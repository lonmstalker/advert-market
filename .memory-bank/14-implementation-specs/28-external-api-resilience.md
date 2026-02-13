# External API Resilience (Circuit Breakers)

## Overview

3 external dependencies with different SLA and failure modes. For each - circuit breaker, fallback, monitoring. Implementation: Resilience4j (Spring Boot integration).

---

## External Dependencies

| Dependency | Purpose | Failure Impact |
|-----------|---------|---------------|
| TON Center API | Deposit detection, payout TX, balance checks | Deposits not detected, payouts stuck |
| Telegram Bot API | Notifications, webhook registration | Users not notified |
| Telegram Platform | WebApp auth (initData), deep links | Login fails |

---

## Resilience4j Configuration

### Gradle

```groovy
dependencies {
    implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-retry:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-timelimiter:2.2.0'
}
```

---

## Circuit Breaker: TON Center API

### Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      ton-center:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 20
        failure-rate-threshold: 50           # Open at 50% failures
        slow-call-rate-threshold: 80         # Open at 80% slow calls
        slow-call-duration-threshold: 10s
        wait-duration-in-open-state: 30s     # Half-open after 30s
        permitted-number-of-calls-in-half-open-state: 5
        minimum-number-of-calls: 10
        record-exceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
          - org.springframework.web.client.HttpServerErrorException
        ignore-exceptions:
          - org.springframework.web.client.HttpClientErrorException$BadRequest
          - org.springframework.web.client.HttpClientErrorException$Unauthorized
```

### State Transitions

```
CLOSED -> (failure rate >= 50%) -> OPEN -> (30s wait) -> HALF_OPEN -> (5 test calls)
                                                                    -> success >= 60% -> CLOSED
                                                                    -> success < 60% -> OPEN
```

### Fallback Behavior

| Operation | Fallback When Circuit Open |
|-----------|---------------------------|
| `getTransactions` (deposit watch) | Skip poll cycle, retry next interval (10s). Log WARN. |
| `sendBoc` (payout/refund) | Queue for retry. Do NOT mark as failed. Alert operator if open > 5 min. |
| `estimateFee` | Use cached fee estimate (last known good, default 0.01 TON). |
| `getMasterchainInfo` | Use cached seqno. Stale seqno = delayed confirmation, not incorrect. |
| `getAddressBalance` | Return cached balance. Flag as stale in response. |

### Critical Rule

**Payout/refund TX (sendBoc) MUST NOT be dropped silently.** If circuit is open:
1. Store TX in `pending_outbound_tx` queue (Redis list)
2. Alert operator
3. When circuit closes: drain queue with dedup check (idempotency by `deal_id + direction`)

---

## Circuit Breaker: Telegram Bot API

### Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      telegram-bot:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 50
        failure-rate-threshold: 40
        slow-call-duration-threshold: 5s
        wait-duration-in-open-state: 15s
        permitted-number-of-calls-in-half-open-state: 10
        minimum-number-of-calls: 20
        record-exceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
        ignore-exceptions:
          - com.pengrad.telegrambot.TelegramException  # 403 blocked, etc.
```

### Fallback Behavior

| Operation | Fallback |
|-----------|----------|
| `sendMessage` (notification) | Mark in outbox as PENDING, retry via outbox poller. Non-blocking. |
| `setWebhook` | Log ERROR, retry on next app restart. |
| `getChatMemberCount` | Return cached count, flag stale. |

### 429 Handling (Rate Limit, NOT Circuit Breaker)

Telegram 429 with `retry_after` is not a failure, but rate control. It does NOT count as a failure for the circuit breaker. Handled separately via `Retry`:

```yaml
resilience4j:
  retry:
    instances:
      telegram-bot:
        max-attempts: 3
        wait-duration: 1s
        retry-on-result: # Custom predicate for 429
        retry-exceptions:
          - java.io.IOException
```

---

## Circuit Breaker: Telegram Platform (Auth)

### Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      telegram-platform:
        sliding-window-type: TIME_BASED
        sliding-window-size: 60              # 60-second window
        failure-rate-threshold: 30           # Strict: auth is critical
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
        minimum-number-of-calls: 5
```

### Fallback Behavior

| Operation | Fallback |
|-----------|----------|
| initData validation | HMAC validation is local (no external call). Circuit breaker applies only if we verify via Telegram API additionally. |

**Note**: initData HMAC validation is computed locally using bot token. No external dependency for basic auth. Circuit breaker here protects optional enhanced validation.

---

## Timeout Configuration

| API | Connect | Read | Total |
|-----|---------|------|-------|
| TON Center | 5s | 30s | 35s |
| Telegram Bot | 5s | 10s | 15s |
| Telegram Platform | 3s | 5s | 8s |

```yaml
resilience4j:
  timelimiter:
    instances:
      ton-center:
        timeout-duration: 35s
      telegram-bot:
        timeout-duration: 15s
```

---

## Bulkhead (Concurrency Isolation)

Prevents one slow dependency from running out of thread pool:

```yaml
resilience4j:
  bulkhead:
    instances:
      ton-center:
        max-concurrent-calls: 10
        max-wait-duration: 2s
      telegram-bot:
        max-concurrent-calls: 30
        max-wait-duration: 1s
```

---

## Monitoring

### Metrics (automatically via Resilience4j + Micrometer)

| Metric | Type | Description |
|--------|------|-------------|
| `resilience4j.circuitbreaker.state` | Gauge | 0=CLOSED, 1=OPEN, 2=HALF_OPEN |
| `resilience4j.circuitbreaker.calls` | Counter | Tags: kind={successful,failed,not_permitted} |
| `resilience4j.circuitbreaker.failure.rate` | Gauge | Current failure rate % |
| `resilience4j.circuitbreaker.slow.call.rate` | Gauge | Current slow call rate % |
| `resilience4j.bulkhead.available.concurrent.calls` | Gauge | Available permits |

### Alerts

| Alert | Condition | Severity |
|-------|-----------|----------|
| TON CB Open | `state == OPEN` for > 2 min | CRITICAL (payouts affected) |
| TON CB Open | `state == OPEN` for > 30s | WARNING |
| Telegram CB Open | `state == OPEN` for > 5 min | WARNING |
| Bulkhead saturated | `available == 0` for > 1 min | WARNING |

### Grafana Dashboard: External Dependencies

- Circuit breaker state timeline (per dependency)
- Failure rate trend
- Call latency percentiles
- Bulkhead utilization

---

## Health Indicators

```java
// Spring Boot Actuator health integration
// /actuator/health shows:
{
  "components": {
    "tonCenter": {
      "status": "UP",
      "details": {
        "circuitBreakerState": "CLOSED",
        "failureRate": 2.5,
        "lastSuccessfulCall": "2025-01-15T10:29:55Z"
      }
    },
    "telegramBot": {
      "status": "UP",
      "details": {
        "circuitBreakerState": "CLOSED",
        "failureRate": 0.0
      }
    }
  }
}
```

---

## Degraded Mode Behavior

When the circuit is open, the system continues to operate in degraded mode:

| Dependency Down | What Works | What Doesn't |
|----------------|------------|-------------|
| TON Center | All non-financial operations, deal creation, creative flow | Deposit detection, payout, refund |
| Telegram Bot | All API operations, financial operations | Notifications (queued in outbox) |
| Both | API operations, state transitions | Deposits, payouts, notifications |

---

## Related Documents

- [TON SDK Integration](./01-ton-sdk-integration.md)
- [Telegram Bot Framework](./02-telegram-bot-framework.md)
- [Deployment Runbook](./23-deployment-runbook.md)
- [Metrics & SLOs](./21-metrics-slos-monitoring.md)
- [Rate Limiting Strategy](./27-rate-limiting-strategy.md)
