# Pattern: Double-Entry Ledger

## Decision

Use double-entry bookkeeping for all financial operations to ensure money never appears or disappears.

## Context

The platform holds other people's money (escrow). A single-entry system (just recording amounts) makes it easy to have off-by-one errors, lost funds, or phantom balances. Double-entry bookkeeping enforces the invariant: **total debits = total credits**, always.

## Principle

Every financial operation creates exactly **two entries**:
- One **debit** (money leaving an account)
- One **credit** (money entering an account)

Both entries share the same `tx_ref` (transaction reference) and are written in the same database transaction.

## Invariant

```
SUM(all debits) = SUM(all credits)
```

If this invariant is ever violated, the Reconciliation Service detects it.

## ledger_entries Structure (matches DDL)

| Column | Type | Description |
|--------|------|-------------|
| `id` | `BIGSERIAL` | Entry ID (efficient for partitioned tables) |
| `tx_ref` | `UUID` | Groups the debit+credit pair |
| `account_id` | `VARCHAR(100)` | Account identifier (e.g., `ESCROW:{deal_id}`) |
| `entry_type` | `VARCHAR(30)` | Operation type (e.g., `ESCROW_DEPOSIT`) |
| `debit_nano` | `BIGINT` | Debit amount in nanoTON (0 if credit entry) |
| `credit_nano` | `BIGINT` | Credit amount in nanoTON (0 if debit entry) |
| `description` | `VARCHAR(500)` | Human-readable description |
| `deal_id` | `UUID` | Deal reference (nullable) |
| `idempotency_key` | `VARCHAR(200)` | Prevents duplicate entries |
| `created_at` | `TIMESTAMPTZ` | Entry timestamp (partition key) |

> **Design note**: `debit_nano/credit_nano` columns (not `direction + amount_nano`) for simpler SQL aggregation: `SUM(debit_nano)`, `SUM(credit_nano)`. DB-level CHECK ensures exactly one is positive per row.

## Example: Escrow Deposit

When an advertiser deposits 1000 TON (1,000,000,000,000 nanoTON):

| tx_ref | account_id | debit_nano | credit_nano | entry_type |
|--------|-----------|------------|-------------|------------|
| `uuid-1` | `EXTERNAL_TON` | 1000000000000 | 0 | ESCROW_DEPOSIT |
| `uuid-1` | `ESCROW:deal-123` | 0 | 1000000000000 | ESCROW_DEPOSIT |

## Example: Escrow Release with Commission

When delivery is verified and escrow is released (1000 TON deal, 10% commission):

| tx_ref | account_id | debit_nano | credit_nano | entry_type |
|--------|-----------|------------|-------------|------------|
| `uuid-2` | `ESCROW:deal-123` | 1000000000000 | 0 | ESCROW_RELEASE |
| `uuid-2` | `COMMISSION:deal-123` | 0 | 100000000000 | PLATFORM_COMMISSION |
| `uuid-2` | `OWNER_PENDING:owner-456` | 0 | 900000000000 | OWNER_PAYOUT |

Note: For 3-entry operations (split), the `tx_ref` groups all entries together. The invariant still holds: 1000 = 100 + 900.

## Example: Refund

When a dispute is resolved in favor of the advertiser:

| tx_ref | account_id | debit_nano | credit_nano | entry_type |
|--------|-----------|------------|-------------|------------|
| `uuid-3` | `ESCROW:deal-123` | 1000000000000 | 0 | ESCROW_REFUND |
| `uuid-3` | `EXTERNAL_TON` | 0 | 999995000000 | ESCROW_REFUND |
| `uuid-3` | `NETWORK_FEES` | 0 | 5000000 | NETWORK_FEE_REFUND |

## Benefits

| Benefit | Description |
|---------|-------------|
| **Self-balancing** | System detects errors automatically via the invariant |
| **Complete trail** | Every nanoTON movement is traceable |
| **Reconciliation** | Three-way comparison possible: ledger vs blockchain vs deal aggregates |
| **Reporting** | Account statements derived directly from ledger |
| **Compliance** | Standard accounting practice, familiar to auditors |

## Enforcement

- `ledger_entries` is **append-only** (no UPDATE/DELETE)
- Every write operation creates entries summing to zero (`SUM(debits) - SUM(credits) = 0` per `tx_ref`)
- Reconciliation Service periodically verifies the global invariant

## Related Documents

- [Ledger Design](../07-financial-system/01-ledger-design.md) — detailed schema
- [Account Types](../07-financial-system/05-account-types.md) — all account identifiers
- [CQRS](./02-cqrs.md) — read model projection from ledger
- [Escrow Flow](../07-financial-system/02-escrow-flow.md)
- [Reconciliation](../07-financial-system/04-reconciliation.md)
