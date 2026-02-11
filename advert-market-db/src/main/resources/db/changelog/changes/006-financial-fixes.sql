--liquibase formatted sql

--changeset advert-market:006-financial-fixes

-- EXTERNAL_TON is a contra-account that goes negative on every deposit
-- Non-negative balance enforced at application level for specific account types
ALTER TABLE account_balances DROP CONSTRAINT account_balances_balance_nano_check;

-- tx_ref groups related entries into a logical transaction (debit+credit pair)
-- Invariant: SUM(debits) = SUM(credits) per tx_ref
ALTER TABLE ledger_entries ADD COLUMN tx_ref UUID NOT NULL;
CREATE INDEX idx_ledger_tx_ref ON ledger_entries(tx_ref);

-- DB sequence for subwallet_id generation (unique per deal)
CREATE SEQUENCE deal_subwallet_seq START 1;
ALTER TABLE deals ADD CONSTRAINT uq_deals_subwallet_id UNIQUE (subwallet_id);

-- Deal columns for escrow flow
ALTER TABLE deals ADD COLUMN funded_at TIMESTAMPTZ;
ALTER TABLE deals ADD COLUMN deposit_tx_hash VARCHAR(100);

-- Distinguish payouts from refunds in reconciliation
ALTER TABLE ton_transactions ADD COLUMN tx_type VARCHAR(20)
    CHECK (tx_type IN ('DEPOSIT', 'PAYOUT', 'REFUND', 'OVERPAYMENT_REFUND'));

-- Optional description for ledger entries
ALTER TABLE ledger_entries ADD COLUMN description VARCHAR(500);

-- Link audit records to ledger transactions
ALTER TABLE audit_log ADD COLUMN tx_ref UUID;
CREATE INDEX idx_audit_tx_ref ON audit_log(tx_ref) WHERE tx_ref IS NOT NULL;
