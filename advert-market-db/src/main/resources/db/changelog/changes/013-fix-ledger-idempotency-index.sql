--liquibase formatted sql

--changeset advert-market:013-fix-ledger-idempotency-index

-- The UNIQUE index on (idempotency_key, created_at) conflicts with multi-leg transfers:
-- when inserting 3+ legs with the same idempotency_key in one transaction,
-- they share the same created_at timestamp, violating the unique constraint.
-- ledger_idempotency_keys.PK is the sole source of exactly-once guarantee.
DROP INDEX IF EXISTS idx_ledger_idempotency;
CREATE INDEX idx_ledger_entries_idempotency ON ledger_entries(idempotency_key);
