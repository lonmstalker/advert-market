--liquibase formatted sql

--changeset channel-search:009-01
--comment: Enable pg_search extension for BM25 full-text search
CREATE EXTENSION IF NOT EXISTS pg_search;

--changeset channel-search:009-02
--comment: Add engagement_rate, avg_views, language columns to channels
ALTER TABLE channels
    ADD COLUMN engagement_rate DECIMAL(5, 2),
    ADD COLUMN avg_views       INTEGER     DEFAULT 0,
    ADD COLUMN language        VARCHAR(10) DEFAULT 'ru';

--changeset channel-search:009-03
--comment: Create BM25 index for full-text search via ParadeDB pg_search
CREATE INDEX idx_channels_bm25 ON channels
    USING bm25 (
        id,
        title,
        description,
        category
    )
    WITH (key_field = 'id');

--changeset channel-search:009-04
--comment: Keyset pagination indexes (partial, WHERE is_active = true)
CREATE INDEX idx_channels_active_subscribers
    ON channels (subscriber_count DESC, id DESC)
    WHERE is_active;

CREATE INDEX idx_channels_active_price
    ON channels (price_per_post_nano ASC, id ASC)
    WHERE is_active;

CREATE INDEX idx_channels_active_engagement
    ON channels (engagement_rate DESC, id DESC)
    WHERE is_active;

CREATE INDEX idx_channels_active_updated
    ON channels (updated_at DESC, id DESC)
    WHERE is_active;

CREATE INDEX idx_channels_topic
    ON channels (category, subscriber_count DESC)
    WHERE is_active;

--changeset channel-search:009-05
--comment: Drop old redundant indexes replaced by keyset indexes
DROP INDEX IF EXISTS idx_channels_active;
DROP INDEX IF EXISTS idx_channels_category;
