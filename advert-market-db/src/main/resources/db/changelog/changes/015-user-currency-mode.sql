--liquibase formatted sql

--changeset user-settings:015-01
--comment: Add currency mode to users with AUTO default
ALTER TABLE users
    ADD COLUMN currency_mode VARCHAR(10) NOT NULL DEFAULT 'AUTO';

--changeset user-settings:015-02
--comment: Backfill legacy users into AUTO currency mode
UPDATE users
SET currency_mode = 'AUTO'
WHERE currency_mode IS NULL;

--changeset user-settings:015-03
--comment: Restrict currency mode domain to AUTO/MANUAL
ALTER TABLE users
    ADD CONSTRAINT chk_users_currency_mode
        CHECK (currency_mode IN ('AUTO', 'MANUAL'));
