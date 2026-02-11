# Unclaimed Payouts

## Overview

Handles the scenario where a channel owner has earned funds (deal completed, payout ready) but has not provided a TON withdrawal address.

## Problem

When a deal completes:
1. Escrow is released -> OWNER_PENDING:{user_id} credited
2. Payout executor looks up owner's TON address from `pii_store`
3. If no address found -> funds stuck in OWNER_PENDING indefinitely

## Lifecycle

```
OWNER_PENDING credited
    |
    v
[Has TON address?] --Yes--> Execute payout (normal flow)
    |
    No
    v
[Notify: "Set withdrawal address"]
    |
    v
[Reminder: Day 7, Day 14, Day 21]
    |
    v
[Day 30: Final notice]
    |
    v
[Day 30+: Operator review]
    |
    v
[Operator decides: extend / refund to advertiser / hold]
```

## Notification Schedule

| Day | Channel | Message |
|-----|---------|---------|
| 0 | Telegram Bot | "Deal completed! Set your TON address to receive {amount} TON." |
| 7 | Telegram Bot | "Reminder: {amount} TON waiting. Set your withdrawal address." |
| 14 | Telegram Bot | "Second reminder: {amount} TON unclaimed for 14 days." |
| 21 | Telegram Bot | "Final reminder: {amount} TON will require operator review after 30 days." |
| 30 | Telegram Bot + Operator alert | "Unclaimed payout for {amount} TON (30 days). Operator review required." |

## Operator Actions (Day 30+)

| Action | Description | Ledger |
|--------|-------------|--------|
| **Extend** | Reset 30-day timer, continue reminders | No ledger change |
| **Refund to advertiser** | Return funds to advertiser | DEBIT OWNER_PENDING, CREDIT EXTERNAL_TON |
| **Hold indefinitely** | Mark as "operator-held" | No ledger change, flag in DB |

## Database

```sql
-- New column on users table (or pii_store)
-- Owner's TON withdrawal address is already in pii_store (field_name='ton_address')

-- Track unclaimed payout state
-- Using existing notification_outbox for scheduled reminders
-- Status tracked via deal_events: PAYOUT_UNCLAIMED, PAYOUT_REMINDER_SENT, etc.
```

## Kafka Events

```
Topic: financial.unclaimed-payouts
Key: {user_id}
Payload: {
  "userId": 123456789,
  "dealId": "...",
  "amountNano": 900000000000,
  "daysSinceCompletion": 7,
  "eventType": "REMINDER_SENT"
}
```

## Worker: UnclaimedPayoutWorker

Scheduled task (daily):

1. Query `account_balances` WHERE `account_id LIKE 'OWNER_PENDING:%'` AND `balance_nano > 0`
2. For each, check if owner has TON address in `pii_store`
3. If address exists -> execute payout (may have been added since last check)
4. If no address -> check last notification date -> send next reminder if due
5. If 30+ days -> alert operator

## Monitoring

| Metric | Type | Alert |
|--------|------|-------|
| `payout.unclaimed.count` | Gauge | > 10 -> WARNING |
| `payout.unclaimed.total_nano` | Gauge | > 10_000 TON -> WARNING |
| `payout.unclaimed.oldest_days` | Gauge | > 30 -> HIGH |

## Related Documents

- [Payout Execution Flow](./30-payout-execution-flow.md)
- [Payout Wallet Architecture](./40-payout-wallet-architecture.md)
- [Notification Templates](./22-notification-templates-i18n.md)
- [Account Types](../07-financial-system/05-account-types.md)
