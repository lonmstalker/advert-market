--liquibase formatted sql

--changeset advert-market:022-channel-custom-rules
--comment: Add owner note storage for channel listing custom rules

ALTER TABLE channels
    ADD COLUMN custom_rules TEXT;

