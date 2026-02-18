# Network Fee Accounting

## Overview

Every outbound TON transaction (payout, refund, commission sweep) incurs a gas fee (~0.005 TON). These fees must be tracked in the ledger to maintain the invariant: `SUM(debits) = SUM(credits)`.

## Runtime Status (2026-02-18)

- Implemented in runtime workers (`PayoutExecutorWorker`, `RefundExecutorWorker`, `CommissionSweepScheduler`):
  - fee entries are recorded with configurable estimate `app.financial.network-fee.default-estimate-nano`.
- Planned follow-up:
  - switch to actual on-chain fee synchronization (`ton_transactions.fee_nano` reconciliation path) in `advert-market-d79`.

## NETWORK_FEES Account

New expense account type tracking all TON gas fees paid by the platform.

| Attribute | Value |
|-----------|-------|
| **Format** | `NETWORK_FEES` |
| **Cardinality** | Singleton |
| **Purpose** | Tracks cumulative gas fees spent on all outbound transactions |
| **Credited by** | Fee recording after each confirmed TX |
| **Normal balance** | Credit (expense increases with credits) |

## Fee Recording Flow

Fees are recorded **after** on-chain TX confirmation, using the **actual** fee from the blockchain (not estimated). All ledger entries (including fee) are written in a single phase after confirmation.

### Step 1: TX Confirmation

When `TxConfirmationWorker` detects a confirmed outbound TX:

1. Read `transaction_fee` from on-chain TX data
2. Record fee in `ton_transactions.fee_nano`

### Step 2: Ledger Entry

Fee is included in the same `tx_ref` as the main operation (payout/refund), or as a separate `tx_ref` for payout gas (since payout entries are written as escrow release, and gas is recorded separately):

### Payout Fee

```
tx_ref: {fee_tx_ref}
DEBIT  PLATFORM_TREASURY  {actual_gas_fee}   -- platform pays from commission
CREDIT NETWORK_FEES       {actual_gas_fee}   -- expense recorded
```

### Refund Fee

Fee reduces the refund amount (advertiser receives `amount - gas_fee`):

```
tx_ref: {refund_tx_ref}  -- same tx_ref as the refund entries
DEBIT  ESCROW:{deal_id}      {deal_amount}
CREDIT EXTERNAL_TON          {deal_amount - gas_fee}
CREDIT NETWORK_FEES          {gas_fee}
```

### Commission Sweep Fee

```
tx_ref: {fee_tx_ref}
DEBIT  PLATFORM_TREASURY  {actual_gas_fee}
CREDIT NETWORK_FEES       {actual_gas_fee}
```

## ton_transactions Column

```sql
-- Already in 006-financial-fixes.sql (tx_type added)
-- Additional column needed for fee tracking:
ALTER TABLE ton_transactions ADD COLUMN fee_nano BIGINT DEFAULT 0;
```

> `fee_nano` added in `007-audit-findings.sql`.

## Reconciliation Check

New check: sum of NETWORK_FEES in ledger must approximately match actual on-chain fees.

```sql
-- Ledger NETWORK_FEES total
SELECT COALESCE(SUM(credit_nano), 0) AS ledger_fees
FROM ledger_entries
WHERE account_id = 'NETWORK_FEES';

-- On-chain fees total
SELECT COALESCE(SUM(fee_nano), 0) AS onchain_fees
FROM ton_transactions
WHERE direction = 'OUT'
  AND status = 'CONFIRMED';
```

**Expected**: `ledger_fees = onchain_fees`
**Severity**: HIGH if mismatch (fee recording bug)

## Monitoring

| Metric | Type | Alert |
|--------|------|-------|
| `ton.fees.total_nano` | Counter | -- |
| `ton.fees.avg_per_tx_nano` | Gauge | > 50_000_000 (0.05 TON) -> WARNING |
| `ton.fees.percent_of_commission` | Gauge | > 5% -> WARNING |

## Related Documents

- [Account Types](../07-financial-system/05-account-types.md)
- [NETWORK_FEES Account](../07-financial-system/08-network-fees-account.md)
- [Payout Wallet Architecture](./40-payout-wallet-architecture.md)
- [Reconciliation SQL](./14-reconciliation-sql.md)
