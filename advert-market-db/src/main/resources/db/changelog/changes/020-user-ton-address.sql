--liquibase formatted sql

--changeset advert-market:020-user-ton-address
--comment: Add TON wallet address to users for payout/refund transfers
ALTER TABLE users ADD COLUMN ton_address VARCHAR(67);
CREATE INDEX idx_users_ton_address ON users (ton_address) WHERE ton_address IS NOT NULL;
