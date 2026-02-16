--liquibase formatted sql

--changeset advert-market:018-audit-fixes-seqno-bigint
--comment: Change seqno column to BIGINT (masterchain seqno exceeds INTEGER range)
ALTER TABLE ton_transactions ALTER COLUMN seqno TYPE BIGINT;

--changeset advert-market:018-audit-fixes-seqno-index
--comment: Unique index for TON deposit deduplication by address+seqno
CREATE UNIQUE INDEX uq_ton_tx_address_seqno
    ON ton_transactions(to_address, seqno)
    WHERE direction = 'IN' AND seqno IS NOT NULL;

--changeset advert-market:018-audit-fixes-outbox-stuck-recovery
--comment: Index for recovering stuck PROCESSING outbox entries
CREATE INDEX idx_outbox_processing_recovery
    ON notification_outbox(status, created_at)
    WHERE status = 'PROCESSING';
