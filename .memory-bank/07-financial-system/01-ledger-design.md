# Financial System: Ledger Design

## Overview

The ledger is the **source of truth** for all money movement in the platform. It is implemented as an append-only, double-entry journal stored in the `ledger_entries` table.

## Table Schema (matches DDL)

```sql
CREATE TABLE ledger_entries (
    id              BIGSERIAL,
    deal_id         UUID,
    account_id      VARCHAR(100) NOT NULL,
    entry_type      VARCHAR(30)  NOT NULL,
    debit_nano      BIGINT NOT NULL DEFAULT 0 CHECK (debit_nano >= 0),
    credit_nano     BIGINT NOT NULL DEFAULT 0 CHECK (credit_nano >= 0),
    tx_ref          UUID NOT NULL,             -- groups debit+credit entries
    description     VARCHAR(500),              -- human-readable audit note
    idempotency_key VARCHAR(200) NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (id, created_at),
    CHECK (debit_nano > 0 OR credit_nano > 0),
    CHECK (NOT (debit_nano > 0 AND credit_nano > 0))
) PARTITION BY RANGE (created_at);
```

### Column Notes

| Column | Description |
|--------|-------------|
| `id` | BIGSERIAL — efficient for partitioned tables (not UUID) |
| `debit_nano` / `credit_nano` | Separate columns — better for SQL aggregation than `direction + amount_nano` |
| `tx_ref` | Groups related entries; invariant: `SUM(debit_nano) = SUM(credit_nano)` per `tx_ref` |
| `description` | Optional human-readable note for audit trail |
| `idempotency_key` | Prevents duplicate entries on retry |

## Key Design Decisions

### Append-Only (Immutable)

- No `UPDATE` or `DELETE` operations are permitted
- Corrections are made by creating **reversal entries**, not by modifying existing records
- Tagged: `#immutable`, `#cqrs-write`

### Partitioned by Time

- Monthly partitions on `created_at`
- Enables efficient range queries for reconciliation
- Cold partitions can be archived to cheaper storage

### Transaction Reference (tx_ref)

- Groups related entries into a logical transaction
- A simple deposit creates 2 entries with the same `tx_ref`
- A release with commission creates 3 entries with the same `tx_ref`
- Invariant: `SUM(debit_nano) = SUM(credit_nano)` per `tx_ref`

### All Amounts in nanoTON

- 1 TON = 10^9 nanoTON
- BIGINT storage — no floating point, no rounding errors
- Each row has either `debit_nano > 0` OR `credit_nano > 0`, never both

### Debit/Credit Columns (not direction + amount)

Chosen over `direction VARCHAR + amount_nano BIGINT` for:
- Simpler SQL aggregation: `SUM(debit_nano)`, `SUM(credit_nano)`
- No string parsing in queries
- DB-level CHECK constraints ensure exactly one is positive

## Entry Patterns

### Pattern: Escrow Deposit

When advertiser deposits 500 TON:

| tx_ref | account_id | debit_nano | credit_nano | entry_type |
|--------|-----------|------------|-------------|------------|
| uuid-tx1 | `EXTERNAL_TON` | 500_000_000_000 | 0 | ESCROW_DEPOSIT |
| uuid-tx1 | `ESCROW:deal-123` | 0 | 500_000_000_000 | ESCROW_DEPOSIT |

### Pattern: Escrow Release (with commission)

When escrow is released after delivery verification:

| tx_ref | account_id | debit_nano | credit_nano | entry_type |
|--------|-----------|------------|-------------|------------|
| uuid-tx2 | `ESCROW:deal-123` | 500_000_000_000 | 0 | ESCROW_RELEASE |
| uuid-tx2 | `COMMISSION:deal-123` | 0 | 50_000_000_000 | PLATFORM_COMMISSION |
| uuid-tx2 | `OWNER_PENDING:owner-456` | 0 | 450_000_000_000 | OWNER_PAYOUT |

Invariant check: 500_000_000_000 = 50_000_000_000 + 450_000_000_000

### Pattern: Refund

When a dispute is resolved with refund:

| tx_ref | account_id | debit_nano | credit_nano | entry_type |
|--------|-----------|------------|-------------|------------|
| uuid-tx3 | `ESCROW:deal-123` | 500_000_000_000 | 0 | ESCROW_REFUND |
| uuid-tx3 | `EXTERNAL_TON` | 0 | 499_995_000_000 | ESCROW_REFUND |
| uuid-tx3 | `NETWORK_FEES` | 0 | 5_000_000 | NETWORK_FEE_REFUND |

### Pattern: Commission Sweep to Treasury

Periodic: move accumulated commission to platform treasury:

| tx_ref | account_id | debit_nano | credit_nano | entry_type |
|--------|-----------|------------|-------------|------------|
| uuid-tx4 | `COMMISSION:deal-123` | 50_000_000_000 | 0 | COMMISSION_SWEEP |
| uuid-tx4 | `PLATFORM_TREASURY` | 0 | 50_000_000_000 | COMMISSION_SWEEP |

### Pattern: Network Fee (after TX confirmation)

| tx_ref | account_id | debit_nano | credit_nano | entry_type |
|--------|-----------|------------|-------------|------------|
| uuid-tx5 | `PLATFORM_TREASURY` | 5_000_000 | 0 | NETWORK_FEE |
| uuid-tx5 | `NETWORK_FEES` | 0 | 5_000_000 | NETWORK_FEE |

## Audit Log

Every ledger operation also records to `audit_log`:

| Column | Type | Description |
|--------|------|-------------|
| `id` | `BIGSERIAL` | Audit record ID |
| `actor_id` | `BIGINT` | Who triggered the operation |
| `action` | `VARCHAR(100)` | `ESCROW_FUND`, `ESCROW_RELEASE`, `REFUND`, etc. |
| `entity_type` | `VARCHAR(50)` | Entity type |
| `entity_id` | `VARCHAR(100)` | Entity reference |
| `tx_ref` | `UUID` | Reference to ledger entries (added in 006-financial-fixes) |
| `old_value` | `JSONB` | Previous state |
| `new_value` | `JSONB` | New state |
| `created_at` | `TIMESTAMPTZ` | Timestamp |

## Related Documents

- [Double-Entry Ledger Pattern](../05-patterns-and-decisions/05-double-entry-ledger.md)
- [Account Types](./05-account-types.md) — all account identifiers
- [Entry Type Catalog](./07-entry-type-catalog.md) — complete entry_type enumeration
- [NETWORK_FEES Account](./08-network-fees-account.md) — gas fee tracking
- [CQRS](../05-patterns-and-decisions/02-cqrs.md) — read model projection
- [Escrow Flow](./02-escrow-flow.md) — how ledger entries flow in escrow operations
- [Reconciliation](./04-reconciliation.md) — invariant verification
