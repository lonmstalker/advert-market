--liquibase formatted sql

--changeset advert-market:007-audit-findings

-- =============================================================================
-- Deep Audit Findings Fixes
-- =============================================================================
-- Resolves: CRIT-NEW-4, HIGH-NEW-2, HIGH-NEW-6
-- =============================================================================

-- CRIT-NEW-4a: Fix default deal status
-- State machine starts with DRAFT, not PENDING_REVIEW
ALTER TABLE deals ALTER COLUMN status SET DEFAULT 'DRAFT';

-- CRIT-NEW-4b: Fix partial indexes that reference wrong terminal status names
-- 'COMPLETED' should be 'COMPLETED_RELEASED', add PARTIALLY_REFUNDED and EXPIRED
DROP INDEX IF EXISTS idx_deals_status;
CREATE INDEX idx_deals_status ON deals(status)
    WHERE status NOT IN ('COMPLETED_RELEASED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'EXPIRED');

DROP INDEX IF EXISTS idx_deals_deadline;
CREATE INDEX idx_deals_deadline ON deals(deadline_at)
    WHERE deadline_at IS NOT NULL
    AND status NOT IN ('COMPLETED_RELEASED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'EXPIRED');

-- HIGH-NEW-2: Add fee_nano to ton_transactions for network fee tracking
-- Stores actual gas fee from on-chain TX confirmation
ALTER TABLE ton_transactions ADD COLUMN fee_nano BIGINT DEFAULT 0;

-- HIGH-NEW-6: Add payout/refund TX hash references to deals
-- Required for linking deal completion/refund to specific blockchain transactions
ALTER TABLE deals ADD COLUMN payout_tx_hash VARCHAR(100);
ALTER TABLE deals ADD COLUMN refunded_tx_hash VARCHAR(100);
