# Payout Execution Flow

## Overview

After delivery verification passes, the system releases escrow funds to the channel owner minus platform commission. This spec details the full payout pipeline from trigger to TON blockchain confirmation.

## Payout Trigger

1. Delivery Verifier confirms post integrity (24h retention check passed)
2. Worker callback: `POST /internal/v1/worker-events` with `delivery_verified`
3. DealTransitionService: `DELIVERY_VERIFYING` -> `COMPLETED_RELEASED`
4. DealWorkflowEngine orchestrates release side-effects

## Ledger Entries (Release)

On release, 3 ledger entries in a single `tx_ref` group:

| Step | Debit | Credit | Amount |
|------|-------|--------|--------|
| Full release | `ESCROW:{deal_id}` | -- | `amount_nano` |
| Commission | -- | `COMMISSION:{deal_id}` | `floor(amount_nano * 0.10)` |
| Owner share | -- | `OWNER_PENDING:{owner_id}` | `amount_nano - commission` |

**Rounding**: `floor()` for commission, remainder to owner. Dust sweep handles sub-nanoTON remainders via `PLATFORM_TREASURY`.

All 3 entries recorded atomically in a single DB transaction before payout command is emitted (crash-safe).

## Payout Command (Kafka)

After ledger entries recorded, OutboxPublisher emits to `escrow.commands`:

```json
{
  "command_type": "EXECUTE_PAYOUT",
  "deal_id": "550e8400-...",
  "owner_id": 123456789,
  "amount_nano": 900000000000,
  "payout_address": "<decrypted from pii_store>"
}
```

## Payout Executor Worker

1. Consume from `escrow.commands` (consumer group `cg-payout-executor`)
2. Acquire Redis lock: `lock:payout:{deal_id}` (TTL 60s)
3. Check idempotency key: `idempotency:payout:{deal_id}` (SET NX EX 86400)
4. If already processed: skip, ack message
5. Decrypt payout address from PII Vault
6. Submit TON transaction via TON Center API (`POST sendBoc`)
7. Record in `ton_transactions`: direction=OUT, status=PENDING
8. Poll for confirmation (1 confirmation for outgoing)
9. On success: callback `POST /internal/v1/worker-events` with `payout_completed`
10. Update `ton_transactions.status` = CONFIRMED
11. Release lock

## Error Handling

| Error | Action |
|-------|--------|
| TON API timeout | Retry with backoff (1s -> 5s -> 30s -> 5m -> 30m), max 5 |
| Insufficient balance (hot wallet) | Alert operator, requeue with delay |
| Invalid payout address | Alert operator, mark deal for manual review |
| Lock acquisition failure | Requeue message (another instance processing) |
| Network partition | DLQ after max retries, operator investigation |

## Confirmation Tracking (Outgoing)

- Outgoing payouts need 1 confirmation (lower risk than deposits)
- Worker polls TON Center API for tx status
- Max wait: 5 minutes, then retry

## Payout Status Updates

| Status | When |
|--------|------|
| PAYOUT_PENDING | After ledger release entries recorded |
| PAYOUT_SENT | After tx submitted to blockchain |
| PAYOUT_CONFIRMED | After 1 confirmation received |

Notification sent at PAYOUT_CONFIRMED: owner gets "Выплата {amount} TON" message.

## Idempotency Guarantees

- Redis lock prevents concurrent execution for same deal
- Idempotency key prevents re-execution after restart
- `ton_transactions.tx_hash` PK prevents duplicate recording
- Ledger entries recorded BEFORE payout command emitted (crash-safe)

## Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `escrow.payout.completed` | Counter | Successful payouts |
| `escrow.payout.failed` | Counter | Failed payouts |
| `escrow.payout.duration` | Timer | Time from request to confirmation |
| `escrow.payout.amount` | Summary | Payout amounts (nanoTON) |

## Related Documents

- [Escrow Payments](../03-feature-specs/04-escrow-payments.md)
- [Redis Distributed Locks](./09-redis-distributed-locks.md)
- [Workers](../04-architecture/04-workers.md)
- [Commission Rounding & Sweep](./25-commission-rounding-sweep.md)