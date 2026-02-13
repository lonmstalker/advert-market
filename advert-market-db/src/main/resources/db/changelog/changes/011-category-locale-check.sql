--liquibase formatted sql

--changeset category-locale:011-01
--comment: Enforce required locales (ru, en) in categories.localized_name
ALTER TABLE categories ADD CONSTRAINT chk_categories_localized_name
    CHECK (localized_name->>'ru' IS NOT NULL AND localized_name->>'en' IS NOT NULL);
