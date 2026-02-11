# Late Deposit Handling

## Overview

Handles the scenario where an advertiser sends TON to a deal's subwallet **after** the deal has moved out of `AWAITING_PAYMENT` (expired, cancelled, or already funded).

## Problem

Without handling:
1. Deposit Watcher detects incoming TX
2. DealTransitionService checks `status == AWAITING_PAYMENT` -> reject (wrong status)
3. TON sits in subwallet with no recovery path
4. Ledger does NOT reflect these funds
5. Reconciliation does NOT detect the issue (no ledger entry to compare against)
6. **Advertiser's money is LOST**

## Detection

### Deposit Watcher Enhancement

When `DepositWatcher` detects an incoming TX for a subwallet:

```
1. Load deal by subwallet_id
2. Read on-chain TX logical time (lt) and timestamp
3. If deal.status == AWAITING_PAYMENT:
     -> Normal flow (transition to FUNDED)
4. If deal.status IN (EXPIRED, CANCELLED, REFUNDED):
     -> Check on-chain TX timestamp vs deal.deadline_at:
        a. If TX timestamp < deal.deadline_at (sent BEFORE expiry, detected AFTER):
           -> Grace deposit: treat as valid, transition to FUNDED if possible
              (requires deal.status reversal by operator or auto-recovery)
           -> If CANCELLED/REFUNDED: late deposit, auto-refund
        b. If TX timestamp >= deal.deadline_at (sent AFTER expiry):
           -> Late deposit: trigger auto-refund
5. If deal.status IN (FUNDED, CREATIVE_SUBMITTED, ...):
     -> Overpayment: existing overpayment flow (spec 31)
```

> **TX timestamp check**: The on-chain TX `created_lt` (logical time) is used to determine when the TX was actually submitted, NOT when it was detected by the Deposit Watcher. This prevents punishing advertisers for TON network delays or platform downtime.

### Reconciliation Check (New)

Daily check: all subwallets with on-chain balance > 0 where deal is in terminal state.

```sql
SELECT
    d.id AS deal_id,
    d.status,
    d.deposit_address,
    d.subwallet_id
FROM deals d
WHERE d.subwallet_id IS NOT NULL
  AND d.status IN ('EXPIRED', 'CANCELLED', 'REFUNDED', 'COMPLETED_RELEASED')
  AND EXISTS (
      SELECT 1 FROM ton_transactions t
      WHERE t.deal_id = d.id
        AND t.direction = 'IN'
        AND t.status = 'CONFIRMED'
  );
-- For each result, check on-chain balance via TON API
-- If balance > dust_threshold (1000 nanoTON) -> ALERT
```

**Severity**: CRITICAL (funds stuck in subwallet)

## Auto-Refund Flow

1. Record late deposit in `ton_transactions` (direction=IN, tx_type=DEPOSIT)
2. Create ledger entries:
   ```
   tx_ref: {late_deposit_tx_ref}
   DEBIT  EXTERNAL_TON              {amount}
   CREDIT LATE_DEPOSIT:{deal_id}    {amount}   -- temporary holding account
   ```
3. Immediately initiate refund:
   ```
   tx_ref: {auto_refund_tx_ref}
   DEBIT  LATE_DEPOSIT:{deal_id}    {amount}
   CREDIT EXTERNAL_TON              {amount - gas_fee}
   CREDIT NETWORK_FEES              {gas_fee}
   ```
4. Submit refund TX to `from_address` of the late deposit
5. Record in `ton_transactions` (direction=OUT, tx_type=REFUND)
6. Notify advertiser via Telegram: "Your deposit to deal X was received after the deal expired. Automatic refund initiated."

## Edge Cases

| Case | Handling |
|------|----------|
| Refund TX fails | Standard retry (spec 01 retry algorithm). Max 3 retries, then ABANDONED -> operator alert |
| Multiple late deposits to same subwallet | Each processed independently with own refund |
| Late deposit amount < gas fee | Record in ledger, mark as dust. Operator decides monthly |
| Late deposit to FUNDED deal | Treated as overpayment (spec 31), not late deposit |

## Kafka Events

```
Topic: deal.late-deposits
Key: {deal_id}
Payload: {
  "dealId": "...",
  "txHash": "...",
  "amount_nano": 1000000000,
  "originalStatus": "EXPIRED",
  "refundTxHash": "...",
  "timestamp": "..."
}
```

## Monitoring

| Metric | Type | Alert |
|--------|------|-------|
| `deal.late_deposit.count` | Counter | any > 0 per day -> INFO |
| `deal.late_deposit.total_nano` | Counter | -- |
| `deal.late_deposit.refund_failed` | Counter | any > 0 -> CRITICAL |

## Related Documents

- [Overpayment & Underpayment](./31-overpayment-underpayment.md)
- [TON SDK Integration](./01-ton-sdk-integration.md)
- [Reconciliation SQL](./14-reconciliation-sql.md)
- [Network Fee Accounting](./38-network-fee-accounting.md)
