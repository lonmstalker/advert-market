--liquibase formatted sql

--changeset advert-market:021-creative-library

CREATE TABLE creative_templates (
    id              UUID PRIMARY KEY,
    owner_user_id   BIGINT      NOT NULL REFERENCES users(id),
    title           VARCHAR(100) NOT NULL,
    draft           JSONB       NOT NULL,
    version         INTEGER     NOT NULL DEFAULT 1,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_creative_templates_owner_updated
    ON creative_templates (owner_user_id, updated_at DESC, id DESC)
    WHERE is_deleted = FALSE;

CREATE TABLE creative_template_versions (
    template_id      UUID        NOT NULL REFERENCES creative_templates(id) ON DELETE CASCADE,
    version          INTEGER     NOT NULL,
    draft            JSONB       NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (template_id, version)
);

CREATE INDEX idx_creative_template_versions_created_at
    ON creative_template_versions (created_at DESC);

CREATE TABLE creative_media_assets (
    id              UUID         PRIMARY KEY,
    owner_user_id   BIGINT       NOT NULL REFERENCES users(id),
    media_type      VARCHAR(20)  NOT NULL CHECK (media_type IN ('PHOTO', 'GIF', 'VIDEO', 'DOCUMENT')),
    url             VARCHAR(2048) NOT NULL,
    thumbnail_url   VARCHAR(2048),
    file_name       VARCHAR(255),
    file_size       VARCHAR(32),
    mime_type       VARCHAR(128),
    size_bytes      BIGINT       CHECK (size_bytes IS NULL OR size_bytes >= 0),
    caption         VARCHAR(1024),
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_creative_media_assets_owner_created
    ON creative_media_assets (owner_user_id, created_at DESC)
    WHERE is_deleted = FALSE;
