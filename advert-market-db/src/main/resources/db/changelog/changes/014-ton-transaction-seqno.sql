--liquibase formatted sql
--changeset advert-market:014-ton-transaction-seqno

ALTER TABLE ton_transactions ADD COLUMN seqno INTEGER;