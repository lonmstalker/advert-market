# Metrics, SLOs & Monitoring Definitions

## Monitoring Stack

| Component | Technology |
|-----------|-----------|
| Metrics collection | Micrometer (Spring Boot) |
| Metrics storage | Prometheus |
| Dashboards | Grafana |
| Alerting | Prometheus Alertmanager |
| Logging | Structured JSON (Logback) |

---

## Metric Catalog

### API Metrics

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `http.server.requests` | Timer | method, uri, status | Request latency + count |
| `api.auth.login` | Counter | status | Login attempts |
| `api.deals.created` | Counter | -- | New deals |
| `api.deals.transition` | Counter | from, to | State transitions |

### Financial Metrics

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `escrow.deposit.confirmed` | Counter | -- | Confirmed deposits |
| `escrow.deposit.amount` | Summary | -- | Deposit amounts (nanoTON) |
| `escrow.payout.completed` | Counter | -- | Completed payouts |
| `escrow.payout.duration` | Timer | -- | Time from request to confirmation |
| `ledger.entries.created` | Counter | entry_type | Ledger entries by type |
| `reconciliation.check` | Counter | check, result | Reconciliation results |

### Worker Metrics

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `worker.deposit.poll` | Timer | -- | Deposit polling duration |
| `worker.delivery.check` | Counter | result | Delivery check results |
| `worker.notification.sent` | Counter | template, status | Notifications sent |
| `worker.notification.failed` | Counter | template, error | Failed notifications |

### Infrastructure Metrics

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `kafka.consumer.lag` | Gauge | group, topic | Consumer lag |
| `outbox.lag` | Gauge | -- | Pending outbox records |
| `redis.lock.acquired` | Counter | resource | Lock acquisitions |
| `redis.lock.failed` | Counter | resource | Failed lock acquisitions |
| `ton.api.requests` | Counter | endpoint, status | TON Center API calls |

---

## SLI / SLO Definitions

### API Latency

| SLI | SLO Target | Measurement |
|-----|-----------|-------------|
| p50 response time | < 100ms | `http.server.requests` timer |
| p95 response time | < 500ms | `http.server.requests` timer |
| p99 response time | < 2s | `http.server.requests` timer |
| Error rate (5xx) | < 0.1% | `http.server.requests` status=5xx / total |

### Deal Completion

| SLI | SLO Target | Measurement |
|-----|-----------|-------------|
| Deal completion rate | > 80% | completed / (completed + cancelled + expired) |
| Avg deal duration | < 72h | completed_at - created_at |
| Dispute rate | < 5% | disputed / total active |

### Financial Operations

| SLI | SLO Target | Measurement |
|-----|-----------|-------------|
| Deposit detection time | < 2 min | confirmed_at - tx_time |
| Payout execution time | < 10 min | payout_confirmed - payout_requested |
| Reconciliation accuracy | 100% | All 4 checks pass |
| Escrow funding time | < 1h | funded_at - payment_sent |

### Notifications

| SLI | SLO Target | Measurement |
|-----|-----------|-------------|
| Delivery rate | > 99% | delivered / total |
| Delivery latency | < 5s | delivered_at - created_at |

---

## Alert Rules

### Critical (page on-call)

| Alert | Condition | Duration |
|-------|-----------|----------|
| API down | `up{job="backend"} == 0` | 1 min |
| Error rate spike | `rate(http_server_requests_total{status=~"5.."}[5m]) > 0.05` | 5 min |
| Reconciliation failure | `reconciliation_check{result="FAIL"} > 0` | immediate |
| Payout stuck | `escrow_payout_duration_seconds{quantile="0.99"} > 600` | 5 min |

### Warning

| Alert | Condition | Duration |
|-------|-----------|----------|
| High latency | `http_server_requests_seconds{quantile="0.95"} > 1` | 5 min |
| Consumer lag | `kafka_consumer_lag > 1000` | 5 min |
| Outbox lag | `outbox_lag > 500` | 5 min |
| DLQ messages | `kafka_consumer_records_consumed_total{topic=~".*\\.dlq"} > 0` | immediate |
| DLQ financial | `kafka_consumer_records_consumed_total{topic="escrow.commands.dlq"} > 0` | immediate (CRITICAL) |
| TON API errors | `rate(ton_api_requests{status!="200"}[5m]) > 0.1` | 5 min |
| Worker heartbeat missing | `time() - worker_heartbeat_timestamp > 60` | 1 min (CRITICAL) |
| Backup age | `time() - backup_last_success_timestamp > 90000` (25h) | immediate |
| Partition missing | `partition_next_month_exists == 0` | immediate (CRITICAL) |

### Consumer Lag Thresholds

| Topic | Warning Threshold | Critical Threshold |
|-------|:-----------------:|:------------------:|
| `escrow.commands` | 100 | 500 |
| `deal.events` | 500 | 2000 |
| `deal.deadlines` | 50 | 200 |
| `delivery.commands` | 200 | 1000 |
| `notifications.outbox` | 500 | 2000 |
| `reconciliation.triggers` | 10 | 50 |

### Alert Routing

| Severity | Channel | Recipient |
|----------|---------|-----------|
| CRITICAL | Telegram Bot message | Platform Operator + on-call |
| WARNING | Telegram Bot message | Platform Operator |
| INFO | Grafana annotation | Dashboard only |

---

## Structured Logging

### JSON Format

```json
{
  "timestamp": "2025-01-15T10:30:00.000Z",
  "level": "INFO",
  "logger": "com.advertmarket.deal.DealService",
  "message": "Deal state transition",
  "correlation_id": "uuid...",
  "deal_id": "550e8400...",
  "from_status": "AWAITING_PAYMENT",
  "to_status": "FUNDED",
  "thread": "virtual-thread-1"
}
```

### MDC Fields

| Field | Source | Always Present |
|-------|--------|---------------|
| `correlation_id` | Request header or generated | Yes |
| `user_id` | JWT claims | When authenticated |
| `deal_id` | Request path or event | When applicable |
| `trace_id` | Micrometer tracing | Yes |

---

## Grafana Dashboard Layout

### Dashboard 1: API Overview

- Request rate (by endpoint)
- Error rate (by status code)
- Latency percentiles (p50, p95, p99)
- Active deals by status

### Dashboard 2: Financial Operations

- Deposits: count, amount, confirmation time
- Payouts: count, amount, execution time
- Ledger balance (total debits vs credits)
- Reconciliation check results

### Dashboard 3: Infrastructure

- Kafka consumer lag by group
- Outbox lag and processing rate
- Redis operations and lock contention
- TON API request rate and errors

---

## Related Documents

- [Deployment](../09-deployment.md)
- [Reconciliation SQL](./14-reconciliation-sql.md)
- [Error Code Catalog](./12-error-code-catalog.md)