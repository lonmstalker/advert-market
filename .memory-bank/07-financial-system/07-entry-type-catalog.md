# Entry Type Catalog

## Overview

Complete enumeration of `entry_type` values used in `ledger_entries`. Each entry type represents a specific financial operation in the double-entry ledger.

## Entry Types

### Deposit Operations

| entry_type | Description | Debit Account | Credit Account |
|------------|-------------|---------------|----------------|
| `ESCROW_DEPOSIT` | Advertiser deposits TON for a deal (full amount) | `EXTERNAL_TON` | `ESCROW:{deal_id}` |
| `PARTIAL_DEPOSIT` | Partial deposit received (underpayment) | `EXTERNAL_TON` | `PARTIAL_DEPOSIT:{deal_id}` |
| `PARTIAL_DEPOSIT_PROMOTE` | Promote partial deposits to escrow (threshold reached) | `PARTIAL_DEPOSIT:{deal_id}` | `ESCROW:{deal_id}` |

### Release Operations

| entry_type | Description | Debit Account | Credit Account |
|------------|-------------|---------------|----------------|
| `ESCROW_RELEASE` | Full escrow released after delivery verification | `ESCROW:{deal_id}` | (split below) |
| `OWNER_PAYOUT` | Owner's share credited from escrow release | `ESCROW:{deal_id}` | `OWNER_PENDING:{user_id}` |
| `PLATFORM_COMMISSION` | Commission credited from escrow release | `ESCROW:{deal_id}` | `COMMISSION:{deal_id}` |

### Refund Operations

| entry_type | Description | Debit Account | Credit Account |
|------------|-------------|---------------|----------------|
| `ESCROW_REFUND` | Full refund to advertiser | `ESCROW:{deal_id}` | `EXTERNAL_TON` |
| `PARTIAL_REFUND` | Partial refund (time-based split) | `ESCROW:{deal_id}` | (split: EXTERNAL_TON + OWNER_PENDING + COMMISSION) |
| `OVERPAYMENT_REFUND` | Refund of overpaid amount | `OVERPAYMENT:{deal_id}` | `EXTERNAL_TON` |
| `LATE_DEPOSIT_REFUND` | Auto-refund of deposit to expired/cancelled deal | `LATE_DEPOSIT:{deal_id}` | `EXTERNAL_TON` |

### Sweep Operations

| entry_type | Description | Debit Account | Credit Account |
|------------|-------------|---------------|----------------|
| `COMMISSION_SWEEP` | Move per-deal commission to treasury | `COMMISSION:{deal_id}` | `PLATFORM_TREASURY` |

### Payout Execution

| entry_type | Description | Debit Account | Credit Account |
|------------|-------------|---------------|----------------|
| `OWNER_WITHDRAWAL` | Owner withdraws to TON address | `OWNER_PENDING:{user_id}` | `EXTERNAL_TON` |

### Fee Operations

| entry_type | Description | Debit Account | Credit Account |
|------------|-------------|---------------|----------------|
| `NETWORK_FEE` | TON gas fee recording | `PLATFORM_TREASURY` | `NETWORK_FEES` |
| `NETWORK_FEE_REFUND` | Gas fee deducted from refund amount | (included in refund) | `NETWORK_FEES` |

### Correction Operations

| entry_type | Description | Debit Account | Credit Account |
|------------|-------------|---------------|----------------|
| `REVERSAL` | Operator-initiated reversal (linked via `tx_ref`) | (opposite of original) | (opposite of original) |
| `FEE_ADJUSTMENT` | Operator-initiated fee correction (manual only) | varies | varies |
| `DUST_WRITEOFF` | Monthly write-off of subwallet dust | `DUST_WRITEOFF` | `NETWORK_FEES` |

> **Note**: `FEE_ADJUSTMENT` is for manual operator corrections only. In normal operation, gas fees are recorded with actual amounts after on-chain TX confirmation (single-phase recording). See [Payout Wallet Architecture](../14-implementation-specs/40-payout-wallet-architecture.md).

## Idempotency Key Patterns

| entry_type | Idempotency Key Format |
|------------|----------------------|
| `ESCROW_DEPOSIT` | `deposit:{tx_hash}` |
| `PARTIAL_DEPOSIT` | `partial-deposit:{deal_id}:{tx_hash}` |
| `PARTIAL_DEPOSIT_PROMOTE` | `promote:{deal_id}` |
| `ESCROW_RELEASE` | `release:{deal_id}` |
| `ESCROW_REFUND` | `refund:{deal_id}` |
| `PARTIAL_REFUND` | `partial-refund:{deal_id}` |
| `OVERPAYMENT_REFUND` | `overpayment-refund:{deal_id}:{tx_hash}` |
| `LATE_DEPOSIT_REFUND` | `late-deposit-refund:{deal_id}:{tx_hash}` |
| `COMMISSION_SWEEP` | `sweep:{date}:{account_id}` |
| `OWNER_WITHDRAWAL` | `withdrawal:{user_id}:{timestamp}` |
| `NETWORK_FEE` | `fee:{tx_hash}` |
| `REVERSAL` | `reversal:{original_tx_ref}` |

## Validation Rules

1. Each `tx_ref` group MUST have `SUM(debit_nano) = SUM(credit_nano)`
2. `entry_type` is informational — it does NOT affect balance calculation
3. Only `debit_nano` / `credit_nano` columns drive balances
4. `entry_type` is used for filtering in reconciliation queries and audit

## Related Documents

- [Ledger Design](./01-ledger-design.md)
- [Account Types](./05-account-types.md)
- [Reconciliation SQL](../14-implementation-specs/14-reconciliation-sql.md)
- [Network Fee Accounting](../14-implementation-specs/38-network-fee-accounting.md)
