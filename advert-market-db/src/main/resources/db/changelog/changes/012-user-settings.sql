--liquibase formatted sql

--changeset user-settings:012-01
--comment: Add display currency column to users
ALTER TABLE users ADD COLUMN display_currency VARCHAR(3) NOT NULL DEFAULT 'USD';

--changeset user-settings:012-02
--comment: Add notification settings JSONB column to users
ALTER TABLE users ADD COLUMN notification_settings JSONB NOT NULL DEFAULT '{
  "deals": {"newOffers": true, "acceptReject": true, "deliveryStatus": true},
  "financial": {"deposits": true, "payouts": true, "escrow": true},
  "disputes": {"opened": true, "resolved": true}
}';
