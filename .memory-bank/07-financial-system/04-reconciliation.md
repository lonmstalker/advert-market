# Financial System: Reconciliation

## Overview

Reconciliation is a periodic three-way comparison that ensures consistency between the platform's internal ledger, the TON blockchain, and deal aggregates. It is the financial safety net — detecting any discrepancies before they become problems.

## Three-Way Reconciliation

```mermaid
flowchart TB
    subgraph "Data Sources"
        LE[(ledger_entries)]
        TT[(ton_transactions)]
        D[(deals)]
    end

    RS[Reconciliation Service] -->|Read| LE
    RS -->|Read| TT
    RS -->|Read| D

    RS --> CHK1{Ledger balanced?<br/>SUM debits = SUM credits}
    RS --> CHK2{Ledger vs TON?<br/>IN/OUT amounts match}
    RS --> CHK3{Ledger vs Deals?<br/>Escrow amounts match deal states}

    CHK1 -->|Mismatch| ALERT[Alert Operator]
    CHK2 -->|Mismatch| ALERT
    CHK3 -->|Mismatch| ALERT

    CHK1 -->|OK| REPORT
    CHK2 -->|OK| REPORT
    CHK3 -->|OK| REPORT
    REPORT[Write Report to audit_log]
```

## Reconciliation Checks

### Check 1: Ledger Self-Balance

**Invariant**: `SUM(all DEBIT amounts) = SUM(all CREDIT amounts)`

```sql
SELECT
    SUM(CASE WHEN direction = 'DEBIT' THEN amount_nano ELSE 0 END) AS total_debits,
    SUM(CASE WHEN direction = 'CREDIT' THEN amount_nano ELSE 0 END) AS total_credits
FROM ledger_entries;
-- total_debits MUST equal total_credits
```

Also per-transaction: for each `tx_ref`, debits = credits.

### Check 2: Ledger vs TON Blockchain

Compares internal records against blockchain reality:

| Check | Ledger Source | Blockchain Source |
|-------|-------------|------------------|
| Total deposits IN | SUM of `EXTERNAL_TON` DEBIT entries | SUM of `ton_transactions` where `direction = IN` |
| Total payouts OUT | SUM of `EXTERNAL_TON` CREDIT entries (payouts) | SUM of `ton_transactions` where `direction = OUT` and type = PAYOUT |
| Total refunds OUT | SUM of `EXTERNAL_TON` CREDIT entries (refunds) | SUM of `ton_transactions` where `direction = OUT` and type = REFUND |

### Check 3: Ledger vs Deal Aggregates

For each deal in a financial state:

| Deal Status | Expected Ledger State |
|-------------|----------------------|
| `FUNDED` | `ESCROW:{deal_id}` balance = `deal.amount_nano` |
| `COMPLETED_RELEASED` | `ESCROW:{deal_id}` balance = 0, payout entries exist |
| `REFUNDED` | `ESCROW:{deal_id}` balance = 0, refund entries exist |
| `CANCELLED` (was funded) | `ESCROW:{deal_id}` balance = 0, refund entries exist |

### Check 4: Account Balance Projection

Verifies CQRS read model consistency:

```sql
-- For each account, verify materialized balance matches ledger
SELECT account_id,
    (SELECT SUM(CASE WHEN direction='CREDIT' THEN amount_nano ELSE -amount_nano END)
     FROM ledger_entries WHERE account_id = ab.account_id) AS calculated,
    ab.balance_nano AS materialized
FROM account_balances ab
WHERE calculated != materialized;
-- Should return 0 rows
```

## Trigger Mechanism

```mermaid
sequenceDiagram
    participant CRON as Scheduled Trigger
    participant KF as reconciliation.triggers
    participant RW as Reconciliation Worker
    participant API as Backend API
    participant RS as Reconciliation Service

    CRON->>KF: Publish trigger event
    RW->>KF: Consume trigger
    RW->>API: POST /internal/worker-events (reconciliation_start)
    API->>RS: Execute reconciliation
    RS->>RS: Run all checks
    alt Discrepancy found
        RS->>RS: Write alert to audit_log
        RS->>RS: Publish notification to outbox
        Note right of RS: Operator receives Telegram alert
    else All OK
        RS->>RS: Write success report to audit_log
    end
```

**Schedule**: Configurable (recommended: every 6 hours in MVP, hourly in Scaled).

## Reconciliation Exclusion Lock

Uses Redis distributed lock `lock:reconciliation` (TTL 300s) to prevent concurrent reconciliation runs.

## Discrepancy Handling

| Severity | Condition | Action |
|----------|-----------|--------|
| **Critical** | Ledger imbalanced (debits != credits) | Immediate operator alert, pause financial operations |
| **High** | Ledger vs TON mismatch | Operator alert, investigate within 1 hour |
| **Medium** | Ledger vs deal state mismatch | Operator alert, investigate within 24 hours |
| **Low** | CQRS projection drift | Auto-correct projection, log event |

## Components Involved

| Component | Role |
|-----------|------|
| **Reconciliation Service** | Reads all three sources, performs checks, writes reports |
| **Reconciliation Worker** | Periodic trigger consumer |
| **audit_log** | Stores reconciliation reports |
| **Bot Notifier** | Delivers alerts to operator |

## Related Documents

- [Ledger Design](./01-ledger-design.md) — ledger structure
- [Double-Entry Ledger](../05-patterns-and-decisions/05-double-entry-ledger.md) — invariant principle
- [CQRS](../05-patterns-and-decisions/02-cqrs.md) — projection verification
- [Workers](../04-architecture/04-workers.md) — Reconciliation Worker
- [Security & Compliance](../10-security-and-compliance.md) — audit requirements
