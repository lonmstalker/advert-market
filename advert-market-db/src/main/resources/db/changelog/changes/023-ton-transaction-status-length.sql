--liquibase formatted sql

--changeset advert-market:023-ton-transaction-status-length
--comment: Widen TON transaction status to support advanced deposit states

ALTER TABLE ton_transactions
    ALTER COLUMN status TYPE VARCHAR(40);
