# Architecture: Workers & Scheduled Pollers

## Overview

The application uses **2 scheduled pollers** and **3 Kafka listeners** running in the same JVM process as the Backend API. There are no separate worker processes — all async processing happens within the monolith.

## Scheduled Pollers

### 1. DepositWatcher (`@Scheduled`)

| Attribute | Value |
|-----------|-------|
| **Module** | `advert-market-financial` |
| **Class** | `DepositWatcher` |
| **External** | TON Center API (`GET account transactions`) |
| **Lock** | Redis distributed lock (`scheduler:deposit-watcher`) |
| **Output** | Outbox event → `financial.events` topic |

Polls TON network for incoming deposits to deal deposit addresses. Applies tiered Confirmation Policy before confirming. Uses masterchain seqno tracking for confirmation counting.

### 2. DealTimeoutScheduler (`@Scheduled`)

| Attribute | Value |
|-----------|-------|
| **Module** | `advert-market-deal` |
| **Class** | `DealTimeoutScheduler` |
| **Lock** | Redis distributed lock (`scheduler:deal-timeout`) |
| **Output** | State transition via `DealTransitionService` |

Auto-expires deals that exceeded their deadline. Transitions to EXPIRED status with reason. Emits refund indication for funded deals.

### 3. OutboxPoller (`@Scheduled`)

| Attribute | Value |
|-----------|-------|
| **Module** | `advert-market-shared` (wired in app) |
| **Class** | `OutboxPoller` |
| **Output** | Kafka topics (via `KafkaOutboxPublisher`) |

Polls `notification_outbox` table for PENDING entries and publishes to Kafka. Includes stuck entry recovery (PROCESSING > 5min → reset to PENDING).

## Kafka Listeners

### 1. FinancialEventListener

| Attribute | Value |
|-----------|-------|
| **Module** | `advert-market-app` |
| **Topic** | `financial.events` |
| **Factory** | `financialKafkaListenerContainerFactory` (5 retries, 5-60s) |

Processes deposit confirmed/failed events, triggers deal state transitions.

### 2. DeliveryEventListener

| Attribute | Value |
|-----------|-------|
| **Module** | `advert-market-app` |
| **Topic** | `delivery.events` |

Processes delivery verification results.

### 3. ReconciliationResultListener

| Attribute | Value |
|-----------|-------|
| **Module** | `advert-market-app` |
| **Topic** | `reconciliation.results` |

Processes reconciliation check results.

## Error Handling & DLQ Strategy

Each Kafka topic has a corresponding DLQ topic: `{topic}.DLT` (Spring Kafka convention). Messages are routed to DLQ after exhausting retries. DLQ events are counted via `dlq.event.sent` metric for alerting.

| Listener | Max Retries | Backoff |
|----------|:-----------:|---------|
| Financial | 5 | 5s → 60s (exponential ×2) |
| Default | 3 | 1s → 30s (exponential ×2) |
| Notification | 3 | 1s → 10s (exponential ×2) |

Non-retryable: `EventDeserializationException`, `JsonException` → sent directly to DLQ.

## Not Yet Implemented

- **Payout Executor** — TON payout execution (`av4.4`)
- **Post Scheduler** — auto-publish creative to channels (`tj7.1`)
- **Delivery Verifier** — post integrity verification (`tj7.2`)
- **Reconciliation Worker** — three-way reconciliation trigger (`4fr.2`)

## MVP Deployment

All pollers and listeners run in the **same JVM process** as the Backend API. Separation into dedicated services is planned for the Scaled deployment phase.

## Related Documents

- [Containers](./02-containers.md) — container overview
- [Kafka Topology](./06-kafka-topology.md) — topics and routing
- [Deployment](../09-deployment.md) — MVP vs Scaled worker deployment
