# Deal Workflow Engine

## Overview

The Deal Workflow Engine is the core orchestrator that executes side-effects after deal state transitions. While DealTransitionService handles the state machine (status validation, actor checks, event emission), the Workflow Engine handles **what happens next**: escrow operations, notifications, deadline scheduling, and delivery commands.

## Architecture

### Separation of Concerns

| Component | Responsibility |
|-----------|---------------|
| DealTransitionService | State validation, actor ABAC check, optimistic locking, event append |
| DealWorkflowEngine | Post-transition side-effects orchestration |

### Invocation Flow

```
DealController / WorkerCallbackController
    -> DealTransitionService.transition(dealId, targetStatus, actorId)
        -> Validate current state + actor permissions
        -> Update deals.status + version (optimistic lock)
        -> Append deal_events record
        -> DealWorkflowEngine.executePostTransition(deal, fromStatus, toStatus)
            -> Side-effects based on toStatus
```

## Side-Effect Registry

| To Status | Side-Effects |
|-----------|-------------|
| PENDING_REVIEW | Notify owner (NEW_OFFER), set deadline (48h) |
| ACCEPTED | Notify advertiser (OFFER_ACCEPTED), generate deposit address |
| REJECTED | Notify advertiser (OFFER_REJECTED), clear deadline |
| AWAITING_PAYMENT | Set deadline (24h) |
| FUNDED | Notify owner (ESCROW_FUNDED), set deadline (72h), record ledger entries |
| CREATIVE_SUBMITTED | Notify advertiser (CREATIVE_SUBMITTED), set deadline (48h) |
| CREATIVE_APPROVED | Notify owner (CREATIVE_APPROVED), emit PUBLISH_POST command |
| REVISION_REQUESTED | Notify owner (REVISION_REQUESTED), reset creative deadline |
| PUBLISHED | Notify advertiser (PUBLISHED), emit VERIFY_DELIVERY, set deadline (24h) |
| COMPLETED_RELEASED | Release escrow, record ledger entries, emit payout command, notify both |
| DISPUTED | Notify both (DISPUTE_OPENED), set deadline (7 days) |
| PARTIALLY_REFUNDED | Record partial refund ledger entries, emit partial payout + refund commands |
| REFUNDED | Record refund ledger entries, emit refund command, notify both |
| EXPIRED | Clear resources, notify both (DEAL_EXPIRED) |
| CANCELLED | If funded: emit refund. Notify other party (DEAL_CANCELLED) |

## Side-Effect Execution Pattern

### Transactional Boundary

1. State transition + event append: **within DB transaction**
2. Side-effects: **after commit** (via `@TransactionalEventListener(phase = AFTER_COMMIT)` or explicit)

Why: if side-effects fail, the state transition is already committed. Side-effects use the outbox pattern for guaranteed delivery.

### Side-Effect Types

| Type | Mechanism | Guarantee |
|------|-----------|-----------|
| Notification | Write to `notification_outbox` -> Kafka -> Bot Notifier | At-least-once (outbox) |
| Escrow operation | Direct call to EscrowService (same TX or outbox) | Exactly-once (ledger idempotency) |
| Delivery command | Write to `notification_outbox` -> Kafka -> Worker | At-least-once (outbox) |
| Deadline set/clear | Direct DB update on `deals.deadline_at` | Same TX as state transition |

### Deadline Management

```
On entry to time-limited state:
  deals.deadline_at = now() + configured_timeout

On exit from state:
  deals.deadline_at = NULL (or new deadline for next state)
```

## Error Handling

| Scenario | Strategy |
|----------|----------|
| Side-effect fails (notification) | Outbox retry handles it automatically |
| Side-effect fails (escrow) | Log error, alert operator, deal stays in new state |
| Multiple side-effects, one fails | Independent execution -- others proceed |
| Duplicate transition (optimistic lock) | 409 Conflict returned to caller |

## Module Dependencies

DealWorkflowEngine (in `deal-impl` module) depends on:

- `financial-api`: EscrowPort interface (escrow operations)
- `identity-api`: user resolution for notifications
- `marketplace-api`: channel info for notifications
- `communication-api`: NotificationPort interface

No compile dependency on `financial-impl`, `marketplace-impl`, etc. (port interfaces only).

## Configuration

```yaml
deal:
  workflow:
    notifications-enabled: true
    deadline-grace-period: 5m
```

## Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `deal.workflow.side-effect.executed` | Counter | Side-effects executed (by type, toStatus) |
| `deal.workflow.side-effect.failed` | Counter | Failed side-effects (by type) |
| `deal.workflow.duration` | Timer | Time to execute all side-effects |

## Related Documents

- [Deal State Machine](../06-deal-state-machine.md)
- [Deal Lifecycle](../03-feature-specs/02-deal-lifecycle.md)
- [Transactional Outbox](../05-patterns-and-decisions/03-transactional-outbox.md)
- [Inter-Module Interaction](./26-inter-module-interaction.md)
- [Deal Timeout Scheduler](./19-deal-timeout-scheduler.md)