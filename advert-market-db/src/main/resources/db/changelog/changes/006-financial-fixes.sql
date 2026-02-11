--liquibase formatted sql

--changeset advert-market:006-financial-fixes

-- =============================================================================
-- Financial Architecture Audit Fixes
-- =============================================================================
-- Resolves: CRIT-1, CRIT-2, CRIT-3, HIGH-1, HIGH-4, M3, M9
-- See: .memory-bank audit report for full context
-- =============================================================================

-- CRIT-1: Remove balance >= 0 constraint from account_balances
-- EXTERNAL_TON is a contra-account that MUST go negative on every deposit
-- (e.g., deposit 1000 TON -> EXTERNAL_TON balance = -1000)
-- Non-negative checks are enforced at application level for specific account types
ALTER TABLE account_balances DROP CONSTRAINT account_balances_balance_nano_check;

-- CRIT-2: Add tx_ref to ledger_entries
-- Groups related entries into a logical transaction (debit+credit pair)
-- Required for: SUM(debits)=SUM(credits) per-transaction invariant,
-- linking debit to credit, reversal entries, per-TX reconciliation
ALTER TABLE ledger_entries ADD COLUMN tx_ref UUID NOT NULL;
CREATE INDEX idx_ledger_tx_ref ON ledger_entries(tx_ref);

-- CRIT-3: Subwallet sequence + unique constraint
-- Replaces Math.abs(dealId.hashCode()) which has:
--   1. Integer.MIN_VALUE overflow (returns negative)
--   2. Birthday paradox collisions at ~65K deals (32-bit space)
-- DB sequence guarantees uniqueness, no collisions
CREATE SEQUENCE deal_subwallet_seq START 1;
ALTER TABLE deals ADD CONSTRAINT uq_deals_subwallet_id UNIQUE (subwallet_id);

-- HIGH-1: Missing deal columns for escrow flow
-- funded_at: required by escrow flow Stage 3
-- deposit_tx_hash: links deal to confirmed TON deposit transaction
ALTER TABLE deals ADD COLUMN funded_at TIMESTAMPTZ;
ALTER TABLE deals ADD COLUMN deposit_tx_hash VARCHAR(100);

-- HIGH-4: tx_type for reconciliation
-- Enables reconciliation Check 2 to distinguish payouts from refunds
ALTER TABLE ton_transactions ADD COLUMN tx_type VARCHAR(20)
    CHECK (tx_type IN ('DEPOSIT', 'PAYOUT', 'REFUND', 'OVERPAYMENT_REFUND'));

-- M3: Description column for audit trail in ledger entries
ALTER TABLE ledger_entries ADD COLUMN description VARCHAR(500);

-- M9: tx_ref in audit_log for linking audit records to ledger transactions
ALTER TABLE audit_log ADD COLUMN tx_ref UUID;
CREATE INDEX idx_audit_tx_ref ON audit_log(tx_ref) WHERE tx_ref IS NOT NULL;
