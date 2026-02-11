# Reconciliation SQL Queries

## Overview

Four reconciliation checks run daily (or on-demand) to detect financial discrepancies. Each check compares two sources of truth and reports mismatches.

---

## Check 1: Ledger Self-Balance

**Rule**: Total debits must equal total credits across all accounts.

```sql
SELECT
    COALESCE(SUM(debit_nano), 0) AS total_debits,
    COALESCE(SUM(credit_nano), 0) AS total_credits,
    COALESCE(SUM(debit_nano), 0) - COALESCE(SUM(credit_nano), 0) AS diff
FROM ledger_entries
WHERE created_at >= :range_start
  AND created_at < :range_end;
```

**Expected**: `diff = 0`
**Tolerance**: 0 (must be exact)
**Severity**: CRITICAL if diff != 0

### All-time check

```sql
SELECT
    COALESCE(SUM(debit_nano), 0) AS total_debits,
    COALESCE(SUM(credit_nano), 0) AS total_credits,
    COALESCE(SUM(debit_nano), 0) - COALESCE(SUM(credit_nano), 0) AS diff
FROM ledger_entries;
```

---

## Check 2: Ledger vs TON Transactions

**Rule**: Sum of ledger entries for deposits must match sum of confirmed TON IN transactions.

```sql
-- Ledger side: total deposits credited to escrow accounts
SELECT
    COALESCE(SUM(credit_nano), 0) AS ledger_deposits
FROM ledger_entries
WHERE entry_type = 'ESCROW_DEPOSIT'
  AND created_at >= :range_start
  AND created_at < :range_end;

-- TON side: total confirmed incoming transactions
SELECT
    COALESCE(SUM(amount_nano), 0) AS ton_deposits
FROM ton_transactions
WHERE direction = 'IN'
  AND status = 'CONFIRMED'
  AND confirmed_at >= :range_start
  AND confirmed_at < :range_end;
```

**Comparison**: `ABS(ledger_deposits - ton_deposits) <= :tolerance`
**Tolerance**: 0 nanoTON (must be exact for deposits)
**Severity**: CRITICAL if mismatch

### Deposits check (including partial deposits)

```sql
-- Ledger side: total deposits (full + partial) debited from EXTERNAL_TON
SELECT
    COALESCE(SUM(debit_nano), 0) AS ledger_deposits
FROM ledger_entries
WHERE entry_type IN ('ESCROW_DEPOSIT', 'PARTIAL_DEPOSIT')
  AND account_id = 'EXTERNAL_TON'
  AND created_at >= :range_start
  AND created_at < :range_end;
```

> **Note**: Using DEBIT on `EXTERNAL_TON` (not CREDIT on ESCROW) captures both full and partial deposits without double-counting promote entries.

### Payouts check

```sql
-- Ledger side: total withdrawals to external TON (actual on-chain payouts)
SELECT
    COALESCE(SUM(credit_nano), 0) AS ledger_payouts
FROM ledger_entries
WHERE entry_type = 'OWNER_WITHDRAWAL'
  AND account_id = 'EXTERNAL_TON'
  AND created_at >= :range_start
  AND created_at < :range_end;

-- TON side: total confirmed outgoing payout transactions
SELECT
    COALESCE(SUM(amount_nano), 0) AS ton_payouts
FROM ton_transactions
WHERE direction = 'OUT'
  AND tx_type = 'PAYOUT'
  AND status = 'CONFIRMED'
  AND confirmed_at >= :range_start
  AND confirmed_at < :range_end;
```

> **Note**: `OWNER_WITHDRAWAL` (OWNER_PENDING -> EXTERNAL_TON) is the correct entry type for on-chain payout comparison, not `OWNER_PAYOUT` which is an internal ledger operation (ESCROW -> OWNER_PENDING).

---

## Check 3: Ledger vs Deal Aggregates

**Rule**: For each completed deal, ledger entries must balance correctly.

```sql
SELECT
    d.id AS deal_id,
    d.amount_nano AS deal_amount,
    d.commission_nano AS deal_commission,
    COALESCE(le_deposit.total, 0) AS ledger_deposit,
    COALESCE(le_payout.total, 0) AS ledger_payout,
    COALESCE(le_commission.total, 0) AS ledger_commission
FROM deals d
LEFT JOIN LATERAL (
    SELECT SUM(credit_nano) AS total
    FROM ledger_entries
    WHERE deal_id = d.id AND entry_type = 'ESCROW_DEPOSIT'
) le_deposit ON true
LEFT JOIN LATERAL (
    SELECT SUM(credit_nano) AS total
    FROM ledger_entries
    WHERE deal_id = d.id AND entry_type = 'OWNER_PAYOUT'
) le_payout ON true
LEFT JOIN LATERAL (
    SELECT SUM(credit_nano) AS total
    FROM ledger_entries
    WHERE deal_id = d.id AND entry_type = 'PLATFORM_COMMISSION'
) le_commission ON true
WHERE d.status = 'COMPLETED_RELEASED'
  AND d.completed_at >= :range_start
  AND d.completed_at < :range_end
HAVING
    d.amount_nano != COALESCE(le_deposit.total, 0)
    OR d.amount_nano - d.commission_nano != COALESCE(le_payout.total, 0)
    OR d.commission_nano != COALESCE(le_commission.total, 0);
```

**Expected**: No rows returned (all deals balanced)
**Severity**: HIGH per mismatched deal

---

## Check 4: CQRS Projection Consistency

**Rule**: `account_balances` must match recalculated balance from `ledger_entries`.

```sql
SELECT
    ab.account_id,
    ab.balance_nano AS cached_balance,
    COALESCE(SUM(le.credit_nano) - SUM(le.debit_nano), 0) AS calculated_balance,
    ab.balance_nano - COALESCE(SUM(le.credit_nano) - SUM(le.debit_nano), 0) AS diff
FROM account_balances ab
LEFT JOIN ledger_entries le ON le.account_id = ab.account_id
GROUP BY ab.account_id, ab.balance_nano
HAVING ab.balance_nano != COALESCE(SUM(le.credit_nano) - SUM(le.debit_nano), 0);
```

**Expected**: No rows returned
**Severity**: HIGH -- stale projections cause incorrect balance displays

### Auto-fix

If mismatch found, recalculate and update:
```sql
UPDATE account_balances ab
SET balance_nano = (
    SELECT COALESCE(SUM(credit_nano) - SUM(debit_nano), 0)
    FROM ledger_entries
    WHERE account_id = ab.account_id
),
updated_at = now()
WHERE account_id = :account_id;
```

---

## Performance Notes

- Ledger self-balance: O(n) scan, benefits from partition pruning by date range
- Ledger vs TON: Two indexed scans, fast
- Ledger vs Deals: Uses LATERAL joins with indexed `deal_id` lookups
- CQRS check: Full scan of `account_balances` (small table), grouped `ledger_entries` scan

### Indexes Used

- `idx_ledger_account(account_id, created_at DESC)` -- for balance calculation
- `idx_ledger_deal(deal_id)` -- for per-deal reconciliation
- `idx_ton_tx_deal(deal_id)` -- for TON comparison

---

## Alert Severity

| Check | Pass | Fail Severity |
|-------|------|---------------|
| Ledger Self-Balance | diff = 0 | CRITICAL |
| Ledger vs TON | diff = 0 | CRITICAL |
| Ledger vs Deals | 0 mismatched | HIGH per deal |
| CQRS Projection | 0 mismatched | HIGH (auto-fixable) |

---

## Check 5: Per-Transaction Balance (tx_ref)

**Rule**: For each `tx_ref`, total debits must equal total credits.

```sql
SELECT
    tx_ref,
    COALESCE(SUM(debit_nano), 0) AS total_debits,
    COALESCE(SUM(credit_nano), 0) AS total_credits,
    COALESCE(SUM(debit_nano), 0) - COALESCE(SUM(credit_nano), 0) AS diff
FROM ledger_entries
WHERE created_at >= :range_start
  AND created_at < :range_end
GROUP BY tx_ref
HAVING COALESCE(SUM(debit_nano), 0) != COALESCE(SUM(credit_nano), 0);
```

**Expected**: No rows returned (every tx_ref balanced)
**Severity**: CRITICAL (broken double-entry for specific transaction)

> **Note**: Requires `tx_ref UUID NOT NULL` column added in `006-financial-fixes.sql`.

---

## Check 6: Stuck Subwallets (Late Deposits)

**Rule**: No subwallet should hold funds for a deal in terminal state.

```sql
SELECT
    d.id AS deal_id,
    d.status,
    d.deposit_address,
    d.subwallet_id
FROM deals d
WHERE d.subwallet_id IS NOT NULL
  AND d.status IN ('EXPIRED', 'CANCELLED', 'REFUNDED', 'COMPLETED_RELEASED', 'PARTIALLY_REFUNDED')
  AND d.deposit_address IS NOT NULL;
-- For each result, check on-chain balance via TON API
-- If balance > dust_threshold (1000 nanoTON) -> ALERT
```

**Expected**: All returned subwallets have on-chain balance ~0
**Severity**: CRITICAL (funds stuck, potential late deposit not handled)

---

## Check 7: Network Fees Consistency

**Rule**: NETWORK_FEES in ledger must match actual on-chain fees.

```sql
-- Ledger NETWORK_FEES total
SELECT COALESCE(SUM(credit_nano) - SUM(debit_nano), 0) AS ledger_fees
FROM ledger_entries
WHERE account_id = 'NETWORK_FEES';

-- On-chain fees total (requires fee_nano column, added in TON integration phase)
SELECT COALESCE(SUM(fee_nano), 0) AS onchain_fees
FROM ton_transactions
WHERE direction = 'OUT'
  AND status = 'CONFIRMED';
```

**Comparison**: `ABS(ledger_fees - onchain_fees) <= :tolerance`
**Tolerance**: 1000 nanoTON (rounding dust)
**Severity**: HIGH if mismatch

---

## Check 8: Partial Deposit Consistency

**Rule**: PARTIAL_DEPOSIT accounts must match cumulative on-chain deposits for deals in AWAITING_PAYMENT.

```sql
-- Ledger side: partial deposit balances
SELECT
    ab.account_id,
    ab.balance_nano AS ledger_partial,
    d.id AS deal_id
FROM account_balances ab
JOIN deals d ON ab.account_id = 'PARTIAL_DEPOSIT:' || d.id
WHERE d.status = 'AWAITING_PAYMENT'
  AND ab.balance_nano > 0;

-- TON side: confirmed deposits for these deals
SELECT
    deal_id,
    COALESCE(SUM(amount_nano), 0) AS onchain_deposits
FROM ton_transactions
WHERE direction = 'IN'
  AND status = 'CONFIRMED'
  AND deal_id IN (:deal_ids)
GROUP BY deal_id;
```

**Comparison**: `ledger_partial = onchain_deposits` for each deal
**Severity**: HIGH (untracked money on subwallet)

---

## Alert Severity (Updated)

| Check | Pass | Fail Severity |
|-------|------|---------------|
| Ledger Self-Balance | diff = 0 | CRITICAL |
| Ledger vs TON | diff = 0 | CRITICAL |
| Ledger vs Deals | 0 mismatched | HIGH per deal |
| CQRS Projection | 0 mismatched | HIGH (auto-fixable) |
| Per-TX Balance (tx_ref) | 0 mismatched | CRITICAL |
| Stuck Subwallets | 0 with balance | CRITICAL |
| Network Fees | diff <= tolerance | HIGH |
| Partial Deposits | ledger = on-chain | HIGH |

---

## Related Documents

- [Reconciliation](../07-financial-system/04-reconciliation.md)
- [Double-Entry Ledger](../05-patterns-and-decisions/05-double-entry-ledger.md)
- [Late Deposit Handling](./39-late-deposit-handling.md)
- [Network Fee Accounting](./38-network-fee-accounting.md)
- [Metrics & SLOs](./21-metrics-slos-monitoring.md)