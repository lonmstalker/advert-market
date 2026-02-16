--liquibase formatted sql

--changeset advert-market:017-deal-scheduled-at
--comment: Add scheduled_at column for post scheduling in deals

ALTER TABLE deals ADD COLUMN scheduled_at TIMESTAMPTZ;

CREATE INDEX idx_deals_scheduled ON deals(scheduled_at)
    WHERE scheduled_at IS NOT NULL
      AND status IN ('CREATIVE_APPROVED', 'SCHEDULED');
