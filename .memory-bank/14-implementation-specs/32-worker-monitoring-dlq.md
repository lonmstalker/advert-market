# Worker Monitoring & DLQ Strategy

## Overview

Workers are Kafka consumers executing async operations (TON blockchain, Telegram API, reconciliation). This spec defines health monitoring, stuck worker detection, and Dead Letter Queue management.

## Worker Heartbeat Mechanism

### Redis Heartbeat Keys

Each worker instance publishes heartbeat:

```
worker:{type}:{instance_id} -> {"timestamp": ..., "status": "RUNNING", "last_message_at": ..., "consumer_lag": ...}
```

TTL: 30 seconds, refreshed every 10 seconds via Spring `@Scheduled`.

### Heartbeat Publisher

- Includes: timestamp, status, last processed message time, current consumer lag
- On graceful shutdown: SET status = "SHUTTING_DOWN" with 60s TTL

### Health Detection

| Condition | Severity | Action |
|-----------|----------|--------|
| Heartbeat missing > 60s | CRITICAL | Alert operator, trigger restart investigation |
| Heartbeat present but no message > 5min | WARNING | Check consumer lag, may be idle |
| Consumer lag > threshold | WARNING/CRITICAL | Scale up or investigate |
| status = SHUTTING_DOWN > 2min | WARNING | Forced shutdown may be needed |

## Consumer Lag Thresholds

| Topic | Warning | Critical |
|-------|:-------:|:--------:|
| `escrow.commands` | 100 | 500 |
| `deal.events` | 500 | 2000 |
| `deal.deadlines` | 50 | 200 |
| `delivery.commands` | 200 | 1000 |
| `notifications.outbox` | 500 | 2000 |
| `reconciliation.triggers` | 10 | 50 |

Lag source: Kafka AdminClient `listConsumerGroupOffsets()` vs `endOffsets()`, exposed via Micrometer `kafka.consumer.lag{group, topic}`.

## Dead Letter Queue (DLQ)

### DLQ Topic Naming

Each topic has a corresponding DLQ: `{topic}.dlq`

### Retry Policy (per worker)

| Worker | Max Retries | Backoff Strategy | DLQ Topic |
|--------|:-----------:|------------------|-----------|
| TON Deposit Watcher | 5 | 1s -> 5s -> 30s -> 5m -> 30m | `escrow.commands.dlq` |
| Payout Executor | 5 | 1s -> 5s -> 30s -> 5m -> 30m | `escrow.commands.dlq` |
| Refund Executor | 5 | 1s -> 5s -> 30s -> 5m -> 30m | `escrow.commands.dlq` |
| Deal Timeout Worker | 3 | 1s -> 5s -> 30s | `deal.deadlines.dlq` |
| Post Scheduler | 3 | 1s -> 10s -> 60s | `delivery.commands.dlq` |
| Delivery Verifier | 3 | 1s -> 10s -> 60s | `delivery.commands.dlq` |
| Reconciliation Worker | 2 | 5s -> 60s | `reconciliation.triggers.dlq` |

### DLQ Message Envelope

```json
{
  "original_topic": "escrow.commands",
  "original_partition": 3,
  "original_offset": 12345,
  "original_key": "deal_550e8400",
  "original_value": { "..." },
  "error_details": {
    "exception_class": "TonApiTimeoutException",
    "message": "TON Center API timeout after 30s",
    "stack_trace": "...(first 500 chars)"
  },
  "retry_count": 5,
  "worker_instance": "payout-executor-1",
  "first_failure_at": "2025-01-15T10:30:00Z",
  "last_failure_at": "2025-01-15T11:45:00Z"
}
```

### DLQ Processing

1. **Alert on arrival**: any DLQ message triggers WARNING. Financial DLQ (`escrow.commands.dlq`) triggers CRITICAL
2. **Manual replay**: operator reviews via admin endpoint
3. **Replay endpoint**: `POST /internal/v1/dlq/replay` -- moves message back to original topic
4. **Batch replay**: `POST /internal/v1/dlq/replay-all?topic={topic}.dlq`
5. **Discard**: `DELETE /internal/v1/dlq/{topic}/{offset}` -- logged in audit_log

### DLQ Retention

- Non-financial DLQ: retained 30 days, then archived
- Financial DLQ: retained indefinitely until operator resolution

## Spring Kafka Error Handler

Using `DefaultErrorHandler` with `DeadLetterPublishingRecoverer`:

- Retries within consumer (in-memory backoff)
- After max retries: publish to DLQ topic
- Retry does NOT commit offset (message reprocessed on restart)

## Actuator Health Endpoint

`/actuator/health` includes:
- Kafka consumer connectivity
- Per-group consumer lag indicator
- Worker heartbeat status (all workers reporting)
- DLQ message count (non-zero = degraded)

## Alert Rules (Prometheus)

| Alert | Condition | Severity |
|-------|-----------|----------|
| WorkerHeartbeatMissing | `time() - worker_heartbeat_timestamp > 60` | CRITICAL |
| WorkerStuckProcessing | heartbeat present, no message > 5min, lag > 0 | WARNING |
| DLQFinancial | messages in `escrow.commands.dlq` | CRITICAL |
| DLQNonFinancial | messages in other `.dlq` topics | WARNING |
| ConsumerLagCritical | lag > critical threshold | CRITICAL |

## Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `worker.heartbeat.timestamp` | Gauge | Last heartbeat per worker |
| `worker.dlq.messages` | Counter | Messages sent to DLQ (by topic) |
| `worker.dlq.replayed` | Counter | Messages replayed from DLQ |
| `worker.retry.count` | Counter | Retry attempts (by worker, error) |

## Related Documents

- [Workers](../04-architecture/04-workers.md)
- [Kafka Topology](../04-architecture/06-kafka-topology.md)
- [Metrics & SLOs](./21-metrics-slos-monitoring.md)
- [Kafka Consumer Error Handling](./18-kafka-consumer-error-handling.md)