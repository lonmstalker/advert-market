--liquibase formatted sql

--changeset nikitakocnev:008-user-soft-delete
--comment: Add soft delete columns to users table

ALTER TABLE users ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;
