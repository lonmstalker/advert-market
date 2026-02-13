# Deployment Runbook & Troubleshooting Guide

## MVP Deployment Architecture

Single VPS with Docker Compose:
- Backend API (Spring Boot JAR in Docker)
- PostgreSQL 18
- Redis 8
- Kafka 4.1 (KRaft, single broker)
- nginx (reverse proxy + TLS termination)

---

## Step-by-Step Deployment

### 1. Prerequisites

| Requirement | Details |
|-------------|---------|
| VPS | 4 vCPU, 8GB RAM, 100GB SSD |
| OS | Ubuntu 24.04 LTS |
| Docker | 27+ |
| Docker Compose | v2 |
| Domain | bot.advertmarket.com, app.advertmarket.com |
| TLS | Let's Encrypt via certbot |

### 2. Environment Setup

```bash
# Clone repository
git clone <repo> /opt/advertmarket
cd /opt/advertmarket

# Create .env file
cp .env.example .env
# Edit .env with production values
```

### 3. Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_PASSWORD` | PostgreSQL password | (generated) |
| `JWT_SECRET` | >= 32 bytes random | (generated) |
| `TELEGRAM_BOT_TOKEN` | BotFather token | `123456:ABC...` |
| `TELEGRAM_WEBHOOK_SECRET` | Random hex | (generated) |
| `TON_API_KEY` | TON Center API key | (from toncenter.com) |
| `TON_WALLET_MNEMONIC` | 24-word mnemonic | (generated, store securely) |
| `PII_ENCRYPTION_KEY` | Base64 32-byte key | (generated) |

### 4. Deploy

```bash
docker compose up -d
# Wait for health checks
docker compose ps
# Run Liquibase migrations (auto on startup)
# Register Telegram webhook
curl -X POST "https://api.telegram.org/bot${BOT_TOKEN}/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://bot.advertmarket.com/api/v1/bot/webhook","secret_token":"${WEBHOOK_SECRET}"}'
```

### 5. Verify

```bash
# Health check
curl https://bot.advertmarket.com/actuator/health
# Expected: {"status":"UP"}

# Kafka topics
docker exec kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

---

## Health Check Endpoints

| Endpoint | Auth | Expected |
|----------|------|----------|
| `/actuator/health` | Public | `{"status":"UP"}` |
| `/actuator/health/db` | Operator | DB connection status |
| `/actuator/health/redis` | Operator | Redis connection status |
| `/actuator/health/kafka` | Operator | Kafka broker status |
| `/actuator/info` | Public | App version, git commit |

---

## Rollback Procedure

### Application Rollback

```bash
# Tag current version
docker compose exec backend java -version  # note current

# Pull previous version
docker compose pull backend:previous-tag

# Restart
docker compose up -d backend
```

### Database Rollback

Liquibase does not auto-rollback. For schema changes:
1. Write a rollback changeset
2. Apply via `liquibase update`
3. Or restore from backup

### Backup Before Deploy

```bash
# PostgreSQL backup
docker exec postgres pg_dump -U app advertmarket > backup_$(date +%Y%m%d_%H%M%S).sql

# Redis backup
docker exec redis redis-cli BGSAVE
```

---

## Top 10 Failure Scenarios

### 1. Application won't start
- **Symptom**: Container restarts repeatedly
- **Check**: `docker logs backend`
- **Common**: Missing env vars, DB connection refused
- **Fix**: Verify `.env`, check PostgreSQL is healthy

### 2. Telegram webhook not receiving updates
- **Check**: `curl https://api.telegram.org/bot{token}/getWebhookInfo`
- **Common**: Invalid TLS cert, wrong URL, secret mismatch
- **Fix**: Re-register webhook, check nginx TLS config

### 3. Deposits not detected
- **Check**: Kafka consumer lag for `cg-deposit-watcher`
- **Common**: TON API key expired, rate limited
- **Fix**: Check TON_API_KEY, verify TON Center is reachable

### 4. Payouts stuck
- **Check**: `deals` table where `status = 'DELIVERY_VERIFIED'` for too long
- **Common**: Insufficient platform wallet balance, TON API errors
- **Fix**: Check wallet balance, review payout executor logs

### 5. Notifications not sending
- **Check**: `notification_outbox` PENDING count
- **Common**: Bot blocked by user (403), rate limited (429)
- **Fix**: Check outbox lag metric, review notification sender logs

### 6. Database connection pool exhausted
- **Symptom**: "Connection not available" errors
- **Check**: `SELECT count(*) FROM pg_stat_activity`
- **Fix**: Increase `hikari.maximum-pool-size`, find long-running queries

### 7. Kafka consumer lag growing
- **Check**: Prometheus `kafka_consumer_lag` metric
- **Common**: Slow consumer, rebalancing storm
- **Fix**: Check consumer logs, verify processing performance

### 8. Redis connection lost
- **Symptom**: Lock acquisition failures, balance cache misses
- **Fix**: Restart Redis, check memory usage, verify connection config

### 9. Reconciliation failure
- **Alert**: RECONCILIATION_ALERT notification
- **Action**: Check which check failed, review reconciliation SQL output
- **Escalation**: Financial discrepancy -> manual investigation

### 10. Disk space full
- **Check**: `df -h`
- **Common**: PostgreSQL WAL accumulation, Docker logs
- **Fix**: Clean old Docker images, rotate logs, expand disk

---

## Financial Incident Escalation

| Severity | Condition | Response Time | Action |
|----------|-----------|---------------|--------|
| P0 | Reconciliation CRITICAL | 30 min | Pause all payouts, investigate |
| P1 | Payout stuck > 1 hour | 1 hour | Manual payout review |
| P2 | Deposit not detected > 30 min | 2 hours | Check TON API, manual verification |
| P3 | Notification failure | 4 hours | Review outbox, retry manually |

### Emergency: Pause All Financial Operations

```bash
# Set Redis flag to pause payouts
docker exec redis redis-cli SET system:pause_payouts 1
# Clear when resolved
docker exec redis redis-cli DEL system:pause_payouts
```

---

## Graceful Shutdown

### Shutdown Order

When receiving SIGTERM (Docker stop, Kubernetes termination):

```
1. Stop accepting new HTTP requests (Spring marks unhealthy)
2. Wait for in-flight HTTP requests to complete (grace period)
3. Stop Kafka consumers (commit offsets, leave consumer group)
4. Wait for in-flight Kafka message processing
5. Flush outbox poller (process remaining batch)
6. Close Redis connections (release all distributed locks)
7. Close database connections (return to pool, pool shutdown)
8. Exit
```

### Spring Boot Configuration

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

  kafka:
    listener:
      # Stop consuming new messages, finish processing current batch
      # Spring Kafka handles this via container.stop()
```

### Implementation

```java
@Component
public class GracefulShutdownHandler implements DisposableBean {

    private final KafkaListenerEndpointRegistry kafkaRegistry;
    private final OutboxPoller outboxPoller;
    private final RedisLockManager lockManager;

    @Override
    public void destroy() {
        log.info("Graceful shutdown initiated");

        // 1. Stop Kafka consumers
        kafkaRegistry.getListenerContainers().forEach(container -> {
            log.info("Stopping Kafka container: {}", container.getListenerId());
            container.stop();
        });

        // 2. Flush outbox
        outboxPoller.flushRemaining();

        // 3. Release distributed locks owned by this instance
        lockManager.releaseAllOwnedLocks();

        log.info("Graceful shutdown completed");
    }
}
```

### Docker Compose

```yaml
services:
  backend:
    stop_grace_period: 45s   # > spring.lifecycle.timeout-per-shutdown-phase
    stop_signal: SIGTERM
```

### Kafka Consumer Shutdown Details

1. `container.stop()` calls `consumer.wakeup()`
2. The current poll() throws a WakeupException
3. Consumer commitSync() for processed offsets
4. Consumer closes the connection, leaves the consumer group
5. Rebalance occurs on the remaining consumers

### Health Check During Shutdown

```
\u0428\u0430\u0433 1: /actuator/health \u0432\u043e\u0437\u0432\u0440\u0430\u0449\u0430\u0435\u0442 503 (OUT_OF_SERVICE)
\u0428\u0430\u0433 2: Load balancer (nginx) \u043f\u0435\u0440\u0435\u0441\u0442\u0430\u0451\u0442 \u043d\u0430\u043f\u0440\u0430\u0432\u043b\u044f\u0442\u044c \u0442\u0440\u0430\u0444\u0438\u043a
\u0428\u0430\u0433 3: In-flight requests \u0437\u0430\u0432\u0435\u0440\u0448\u0430\u044e\u0442\u0441\u044f
\u0428\u0430\u0433 4: Remaining shutdown steps execute
```

### Pre-Stop Hook (Kubernetes, future)

```yaml
lifecycle:
  preStop:
    exec:
      command: ["sh", "-c", "sleep 5"]  # Allow LB to drain
```

### Readiness vs Liveness Probes

| Probe | Endpoint | Purpose | Checks |
|-------|----------|---------|--------|
| Liveness | `/actuator/health/liveness` | Is app alive? | JVM, Spring context |
| Readiness | `/actuator/health/readiness` | Can accept traffic? | DB, Redis, Kafka |

```yaml
# application.yml
management:
  endpoint:
    health:
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState, db, redis, kafka
```

Docker Compose health check (MVP):
```yaml
services:
  backend:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health/readiness"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 30s
```

---

## Related Documents

- [Deployment Architecture](../09-deployment.md)
- [Project Scaffold](./06-project-scaffold.md)
- [Metrics & SLOs](./21-metrics-slos-monitoring.md)
- [Kafka Consumer Error Handling](./18-kafka-consumer-error-handling.md)