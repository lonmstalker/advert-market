--liquibase formatted sql

--changeset advert-market:019-ton-transaction-retry-count
--comment: Add retry_count column for tracking failed processing attempts
ALTER TABLE ton_transactions ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;
