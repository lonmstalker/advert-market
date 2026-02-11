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

## ledger_entries Structure

| Column | Type | Description |
|--------|------|-------------|
| `id` | `UUID` | Entry ID |
| `tx_ref` | `UUID` | Groups the debit+credit pair |
| `account_id` | `VARCHAR` | Account identifier (e.g., `ESCROW:{deal_id}`) |
| `direction` | `VARCHAR` | `DEBIT` or `CREDIT` |
| `amount_nano` | `BIGINT` | Amount in nanoTON (always positive) |
| `deal_id` | `UUID` | Deal reference (nullable) |
| `description` | `VARCHAR` | Human-readable description |
| `created_at` | `TIMESTAMPTZ` | Entry timestamp |

## Example: Escrow Deposit

When an advertiser deposits 1000 TON (1,000,000,000,000 nanoTON):

| tx_ref | account_id | direction | amount_nano | description |
|--------|-----------|-----------|-------------|-------------|
| `uuid-1` | `EXTERNAL_TON` | DEBIT | 1000000000000 | Deposit received from advertiser |
| `uuid-1` | `ESCROW:deal-123` | CREDIT | 1000000000000 | Escrow funded for deal-123 |

## Example: Escrow Release with Commission

When delivery is verified and escrow is released (1000 TON deal, 10% commission):

| tx_ref | account_id | direction | amount_nano | description |
|--------|-----------|-----------|-------------|-------------|
| `uuid-2` | `ESCROW:deal-123` | DEBIT | 1000000000000 | Escrow released |
| `uuid-2` | `COMMISSION:deal-123` | CREDIT | 100000000000 | Platform commission (10%) |
| `uuid-2` | `OWNER_PENDING:owner-456` | CREDIT | 900000000000 | Owner payout pending |

Note: For 3-entry operations (split), the `tx_ref` groups all entries together. The invariant still holds: 1000 = 100 + 900.

## Example: Refund

When a dispute is resolved in favor of the advertiser:

| tx_ref | account_id | direction | amount_nano | description |
|--------|-----------|-----------|-------------|-------------|
| `uuid-3` | `ESCROW:deal-123` | DEBIT | 1000000000000 | Escrow refunded |
| `uuid-3` | `EXTERNAL_TON` | CREDIT | 1000000000000 | Refund to advertiser |

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
