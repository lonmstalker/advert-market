# Outbox Poller Implementation

## Overview

Current implementation uses a shared outbox poller in
`advert-market-shared/src/main/java/com/advertmarket/shared/outbox/OutboxPoller.java`
with repository implementation in
`advert-market-app/src/main/java/com/advertmarket/app/outbox/JooqOutboxRepository.java`.

This file tracks what is implemented now versus remaining work.

## Implemented in HEAD

### Poll Loop

- Scheduler: `@Scheduled(fixedDelayString = "#{@outboxProperties.pollInterval().toMillis()}")`
- Poll metric: `outbox.poll.count`
- Batch fetch: `findPendingBatch(batchSize)`
- Empty batch short-circuit

### Repository Query + Claiming Semantics

`JooqOutboxRepository.findPendingBatch()` currently:

- selects rows where `status = PENDING`
- filters `created_at < now() - 1 second`
- orders by `created_at ASC`
- limits to `batchSize`
- uses `FOR UPDATE SKIP LOCKED`
- claims the selected rows by transitioning `status: PENDING -> PROCESSING` in the same transaction

Rationale:
- avoids duplicate publications when multiple poller instances are running
- keeps "claim" atomic together with row selection

### Delivery and Retry Behavior

For each entry:

1. `publisher.publish(entry)` with timeout from `publishTimeout`
2. On success: `markDelivered(id)` and metric `outbox.published`
3. On failure:
   - if retries exhausted: `markFailed(id)` + metric `outbox.records.failed`
   - otherwise: `incrementRetry(id)` (also transitions `status: PROCESSING -> PENDING`)

Status values in enum:

- `PENDING`
- `PROCESSING` (used for claiming)
- `DELIVERED`
- `FAILED`

### Config (`app.outbox`)

- `pollInterval`
- `batchSize`
- `maxRetries`
- `initialBackoff`
- `publishTimeout`

Defaults:

- poll interval: 500ms
- batch size: 50
- max retries: 3
- initial backoff: 1s
- publish timeout: 5s

### Test Coverage

- Unit: `advert-market-shared/src/test/java/com/advertmarket/shared/outbox/OutboxPollerTest.java`
- Integration: `advert-market-integration-tests/src/test/java/com/advertmarket/integration/shared/OutboxPollerIntegrationTest.java`

## Not Implemented Yet (Tracked)

| Gap | Beads ID | Notes |
|---|---|---|
| PROCESSING TTL recovery (stuck claims after crash) | `advert-market-6wx.4` | Optional but recommended. Example: "PROCESSING older than X" -> revert to PENDING |
| Exponential backoff timing (`initialBackoff`, `2x`, `4x`) | `advert-market-6wx.4` | Current logic retries on next poll cycle without delay schedule |
| Delivered-record cleanup job (7-day retention) | `advert-market-6wx.4` | No scheduled cleanup found in `HEAD` |
| Poll duration metric (`outbox.poll.duration`) | `advert-market-6wx.4` | Not emitted currently |
| Distributed lock around poller instance | `advert-market-6wx.4` | Not implemented; relies on `SKIP LOCKED` |

## Data Model Notes

Current outbox record model in code (`OutboxEntry`) uses:

- `dealId`
- `idempotencyKey`
- `topic`
- `partitionKey`
- `payload`
- `status`
- `retryCount`
- `version`
- `createdAt`
- `processedAt`

Legacy fields such as `recipient_id` and `template` are not part of this model in `HEAD`.

## Related Documents

- [Transactional Outbox Pattern](../05-patterns-and-decisions/03-transactional-outbox.md)
- [Kafka Topology](../04-architecture/06-kafka-topology.md)
- [Worker Monitoring and DLQ](./32-worker-monitoring-dlq.md)
