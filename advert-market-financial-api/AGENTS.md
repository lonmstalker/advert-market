# Parent Instructions
- Parent: `/Users/nikitakocnev/.codex/AGENTS.md`
- This file contains only module-local deltas.

# Financial API — Agent Instructions

Pure API module: commands, result events, and ports for the double-entry ledger, escrow, payouts, and reconciliation.

## Contents

- **Commands** (5): `WatchDepositCommand`, `ExecutePayoutCommand`, `ExecuteRefundCommand`, `SweepCommissionCommand`, `AutoRefundLateDepositCommand`
- **Result events** (8): `DepositConfirmedEvent`, `DepositFailedEvent`, `PayoutCompletedEvent`, `RefundCompletedEvent`, `ReconciliationStartEvent`, `ReconciliationResultEvent`, `ReconciliationCheck`, `ReconciliationCheckResult`
- **Models** (3): `LedgerEntry`, `Leg`, `TransferRequest`
- **Ports** (2): `FinancialEventPort`, `ReconciliationResultPort`
- **Enums** (2): `DepositFailureReason`, `ReconciliationTriggerType`

## Rules

- DTOs are Java records with Jakarta Validation annotations
- Port interfaces use `@NonNull`/`@Nullable` on all parameters and return types
- No Spring/infrastructure dependencies (compileOnly only)
- `@Fenum` for string constants
- Amount fields are `long` nanoTON (1 TON = 1_000_000_000) — NEVER float/double
