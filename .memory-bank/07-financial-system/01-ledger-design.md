# Financial System: Ledger Design

## Overview

The ledger is the **source of truth** for all money movement in the platform. It is implemented as an append-only, double-entry journal stored in the `ledger_entries` table.

## Table Schema

```sql
CREATE TABLE ledger_entries (
    id              UUID NOT NULL,
    tx_ref          UUID NOT NULL,          -- groups debit+credit pair
    account_id      VARCHAR(200) NOT NULL,  -- e.g., ESCROW:{deal_id}
    direction       VARCHAR(6) NOT NULL,    -- DEBIT or CREDIT
    amount_nano     BIGINT NOT NULL,        -- always positive, in nanoTON
    deal_id         UUID,                   -- FK to deals (nullable for treasury ops)
    description     VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
```

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
- Invariant: `SUM(debits) = SUM(credits)` per `tx_ref`

### All Amounts in nanoTON

- 1 TON = 10^9 nanoTON
- BIGINT storage — no floating point, no rounding errors
- All amounts are positive; direction is indicated by the `direction` column

## Entry Patterns

### Pattern: Escrow Deposit

When advertiser deposits 500 TON:

| id | tx_ref | account_id | direction | amount_nano | description |
|----|--------|-----------|-----------|-------------|-------------|
| uuid-a | uuid-tx1 | `EXTERNAL_TON` | DEBIT | 500000000000 | Deposit received for deal-123 |
| uuid-b | uuid-tx1 | `ESCROW:deal-123` | CREDIT | 500000000000 | Escrow funded |

### Pattern: Escrow Release (with commission)

When escrow is released after delivery verification:

| id | tx_ref | account_id | direction | amount_nano | description |
|----|--------|-----------|-----------|-------------|-------------|
| uuid-c | uuid-tx2 | `ESCROW:deal-123` | DEBIT | 500000000000 | Escrow released |
| uuid-d | uuid-tx2 | `COMMISSION:deal-123` | CREDIT | 50000000000 | Commission 10% |
| uuid-e | uuid-tx2 | `OWNER_PENDING:owner-456` | CREDIT | 450000000000 | Owner payout |

Invariant check: 500000000000 = 50000000000 + 450000000000

### Pattern: Refund

When a dispute is resolved with refund:

| id | tx_ref | account_id | direction | amount_nano | description |
|----|--------|-----------|-----------|-------------|-------------|
| uuid-f | uuid-tx3 | `ESCROW:deal-123` | DEBIT | 500000000000 | Escrow refunded |
| uuid-g | uuid-tx3 | `EXTERNAL_TON` | CREDIT | 500000000000 | Refund to advertiser |

### Pattern: Commission Sweep to Treasury

Periodic: move accumulated commission to platform treasury:

| id | tx_ref | account_id | direction | amount_nano | description |
|----|--------|-----------|-----------|-------------|-------------|
| uuid-h | uuid-tx4 | `COMMISSION:deal-123` | DEBIT | 50000000000 | Commission swept |
| uuid-i | uuid-tx4 | `PLATFORM_TREASURY` | CREDIT | 50000000000 | Treasury credit |

## Audit Log

Every ledger operation also records to `audit_log`:

| Column | Type | Description |
|--------|------|-------------|
| `id` | `UUID` | Audit record ID |
| `actor_type` | `VARCHAR` | `SYSTEM`, `USER`, `OPERATOR` |
| `actor_id` | `BIGINT` | Who triggered the operation |
| `operation` | `VARCHAR` | `ESCROW_FUND`, `ESCROW_RELEASE`, `REFUND`, etc. |
| `tx_ref` | `UUID` | Reference to ledger entries |
| `deal_id` | `UUID` | Deal reference |
| `payload` | `JSONB` | Full operation details |
| `created_at` | `TIMESTAMPTZ` | Timestamp |

## Related Documents

- [Double-Entry Ledger Pattern](../05-patterns-and-decisions/05-double-entry-ledger.md)
- [Account Types](./05-account-types.md) — all account identifiers
- [CQRS](../05-patterns-and-decisions/02-cqrs.md) — read model projection
- [Escrow Flow](./02-escrow-flow.md) — how ledger entries flow in escrow operations
- [Reconciliation](./04-reconciliation.md) — invariant verification
