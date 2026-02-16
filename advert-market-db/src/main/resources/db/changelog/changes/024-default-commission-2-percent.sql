--liquibase formatted sql

--changeset advert-market:024-default-commission-2-percent

ALTER TABLE deals
    ALTER COLUMN commission_rate_bp SET DEFAULT 200;

UPDATE commission_tiers
SET rate_bp = 200,
    description = 'Default 2%',
    version = version + 1
WHERE min_amount_nano = 0
  AND max_amount_nano IS NULL
  AND rate_bp = 1000;

COMMENT ON COLUMN deals.commission_rate_bp IS 'Platform commission in basis points (200 bp = 2%)';
