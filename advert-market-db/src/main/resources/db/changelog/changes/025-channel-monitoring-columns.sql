--liquibase formatted sql

--changeset advert-market:025-channel-monitoring-columns
--comment: Add monitoring timestamps for channel stats and admin verification
ALTER TABLE channels
    ADD COLUMN IF NOT EXISTS stats_updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS bot_verified_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_channels_stats_updated
    ON channels (stats_updated_at NULLS FIRST)
    WHERE is_active;

CREATE INDEX IF NOT EXISTS idx_channels_bot_verified
    ON channels (bot_verified_at NULLS FIRST)
    WHERE is_active;
