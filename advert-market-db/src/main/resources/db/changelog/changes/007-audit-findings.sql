--liquibase formatted sql

--changeset advert-market:007-audit-findings

-- Fix default deal status (state machine starts with DRAFT)
ALTER TABLE deals ALTER COLUMN status SET DEFAULT 'DRAFT';

-- Fix partial indexes with correct terminal status names
DROP INDEX IF EXISTS idx_deals_status;
CREATE INDEX idx_deals_status ON deals(status)
    WHERE status NOT IN ('COMPLETED_RELEASED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'EXPIRED');

DROP INDEX IF EXISTS idx_deals_deadline;
CREATE INDEX idx_deals_deadline ON deals(deadline_at)
    WHERE deadline_at IS NOT NULL
    AND status NOT IN ('COMPLETED_RELEASED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'EXPIRED');

-- Actual gas fee from on-chain TX confirmation
ALTER TABLE ton_transactions ADD COLUMN fee_nano BIGINT DEFAULT 0;

-- Link deal completion/refund to blockchain transactions
ALTER TABLE deals ADD COLUMN payout_tx_hash VARCHAR(100);
ALTER TABLE deals ADD COLUMN refunded_tx_hash VARCHAR(100);
