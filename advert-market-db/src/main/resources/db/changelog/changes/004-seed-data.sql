--liquibase formatted sql

--changeset advert-market:004-seed-commission-tiers

INSERT INTO commission_tiers (min_amount_nano, max_amount_nano, rate_bp, description)
VALUES (0, NULL, 1000, 'Default 10%');
