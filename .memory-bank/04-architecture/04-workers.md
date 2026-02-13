# Architecture: Workers

## Overview

The Workers container hosts 7 async processors that consume Kafka messages and perform background operations. All workers follow the **callback pattern**: after completing their task, they report results back to the Backend API via the Worker Callback Controller (`POST /internal/v1/worker-events`).

## Worker Callback Pattern

```mermaid
flowchart LR
    API[Backend API] -->|Outbox Publisher| KF[Kafka Topic]
    KF -->|Consume| W[Worker]
    W -->|External call| EXT[External System]
    W -->|POST result| CB[Worker Callback Controller]
    CB -->|Trigger| DTS[Deal Transition Service]
```

All workers:
1. Consume from a specific Kafka topic
2. Perform the operation (blockchain call, Telegram API call, etc.)
3. Report result back to Backend API via internal REST endpoint
4. Backend API processes the result and triggers state transitions

## Workers

### 1. TON Deposit Watcher

| Attribute | Value |
|-----------|-------|
| **Tags** | `#financial`, `#blockchain` |
| **Consumes** | `escrow.commands` |
| **Produces** | `escrow.confirmations` |
| **External** | TON Center API (`GET account transactions`) |
| **Callback** | `POST deposit confirmed (tx_hash, amount)` |

Polls TON network for incoming deposits to deal deposit addresses. Applies tiered [Confirmation Policy](../07-financial-system/06-confirmation-policy.md) before confirming.

### 2. Payout Executor

| Attribute | Value |
|-----------|-------|
| **Tags** | `#financial`, `#idempotent` |
| **Consumes** | `escrow.commands` |
| **External** | TON Center API (`POST send transaction`) |
| **Callback** | `POST payout completed (tx_hash)` |

Executes TON payouts to channel owners after commission deduction. Idempotent — uses Redis distributed locks to prevent double execution.

### 3. Refund Executor

| Attribute | Value |
|-----------|-------|
| **Tags** | `#financial`, `#idempotent` |
| **Consumes** | `escrow.commands` |
| **External** | TON Center API (`POST send refund transaction`) |
| **Callback** | `POST refund completed (tx_hash)` |

Executes TON refunds to advertisers on dispute resolution or deal cancellation.

### 4. Deal Timeout Worker

| Attribute | Value |
|-----------|-------|
| **Tags** | `#financial` |
| **Consumes** | `deal.deadlines` |
| **Callback** | Triggers timeout transition |

Auto-cancels/refunds deals stuck beyond configured deadline. See [Deal State Machine — Timeouts](../06-deal-state-machine.md#timeouts).

### 5. Post Scheduler

| Attribute | Value |
|-----------|-------|
| **Consumes** | `delivery.commands` |
| **External** | Telegram Platform (`POST sendMessage to channel`) |
| **Callback** | `POST publication result` |

Auto-publishes approved creative to Telegram channel at the `scheduled_at` timestamp (or immediately).

### 6. Delivery Verifier

| Attribute | Value |
|-----------|-------|
| **Consumes** | `delivery.commands` |
| **Produces** | `delivery.results` |
| **External** | Telegram Platform (`GET message for verification`) |
| **Callback** | `POST verification result` |

Checks published post integrity: tamper/deletion detection over 24h retention period. See [Delivery Verification](../03-feature-specs/05-delivery-verification.md).

### 7. Reconciliation Worker

| Attribute | Value |
|-----------|-------|
| **Tags** | `#financial`, `#audit` |
| **Consumes** | `reconciliation.triggers` |
| **Callback** | `POST reconciliation_start` |

Periodic trigger for three-way reconciliation: ledger vs blockchain vs deal aggregates. See [Reconciliation](../07-financial-system/04-reconciliation.md).

## Kafka Consumer Groups

Each worker has its own consumer group for independent scaling:

| Worker | Consumer Group | Topic(s) |
|--------|---------------|----------|
| TON Deposit Watcher | `cg-deposit-watcher` | `escrow.commands` |
| Payout Executor | `cg-payout-executor` | `escrow.commands` |
| Refund Executor | `cg-refund-executor` | `escrow.commands` |
| Deal Timeout Worker | `cg-deal-timeout` | `deal.deadlines` |
| Post Scheduler | `cg-post-scheduler` | `delivery.commands` |
| Delivery Verifier | `cg-delivery-verifier` | `delivery.commands` |
| Reconciliation Worker | `cg-reconciliation` | `reconciliation.triggers` |

## Error Handling & DLQ Strategy

### Dead Letter Queue (DLQ)

Each Kafka topic has a corresponding DLQ topic: `{topic}.dlq`. Messages are routed to DLQ after exhausting retries.

| Worker | Max Retries | Backoff | DLQ Topic |
|--------|:-----------:|---------|-----------|
| TON Deposit Watcher | 5 | 1s → 5s → 30s → 5m → 30m | `escrow.commands.dlq` |
| Payout Executor | 5 | 1s → 5s → 30s → 5m → 30m | `escrow.commands.dlq` |
| Refund Executor | 5 | 1s → 5s → 30s → 5m → 30m | `escrow.commands.dlq` |
| Deal Timeout Worker | 3 | 1s → 5s → 30s | `deal.deadlines.dlq` |
| Post Scheduler | 3 | 1s → 10s → 60s | `delivery.commands.dlq` |
| Delivery Verifier | 3 | 1s → 10s → 60s | `delivery.commands.dlq` |
| Reconciliation Worker | 2 | 5s → 60s | `reconciliation.triggers.dlq` |

DLQ message envelope includes: original message, error details, retry count, worker instance, timestamp.

### Health Checks

- Each worker publishes heartbeat to Redis: `worker:{type}:{instance}` with 30s TTL
- Missing heartbeat for 60s triggers CRITICAL alert
- Spring Boot Actuator `/actuator/health` includes Kafka consumer group lag
- See [Worker Monitoring & DLQ](../14-implementation-specs/32-worker-monitoring-dlq.md) for full spec

## MVP Deployment

In the MVP deployment, all workers run in the **same JVM process** as the Backend API to minimize operational complexity. They are separated into their own hosts in the Scaled deployment for blast radius isolation.

## Related Documents

- [Containers](./02-containers.md) — container overview
- [Kafka Topology](./06-kafka-topology.md) — topics and routing
- [Deployment](../09-deployment.md) — MVP vs Scaled worker deployment
- [Post Scheduler Worker](../14-implementation-specs/44-post-scheduler-impl.md) — publish flow, retries, idempotency
- [Idempotency Strategy](../05-patterns-and-decisions/07-idempotency-strategy.md)
- [Worker Monitoring & DLQ](../14-implementation-specs/32-worker-monitoring-dlq.md) — health checks, DLQ, alerting
