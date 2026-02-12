-- Simplified DDL for jOOQ code generation.
-- Generated from Liquibase changelogs, stripped of PostgreSQL-specific
-- features unsupported by the jOOQ DDL parser (PARTITION BY, PL/pgSQL, etc.).
-- Source of truth: db/changelog/changes/*.sql

-- 1. users
CREATE TABLE users (
    id                    BIGINT        PRIMARY KEY,
    username              VARCHAR(255),
    first_name            VARCHAR(255)  NOT NULL,
    last_name             VARCHAR(255),
    language_code         VARCHAR(5)    DEFAULT 'ru',
    is_operator           BOOLEAN       DEFAULT FALSE,
    onboarding_completed  BOOLEAN       DEFAULT FALSE,
    interests             TEXT ARRAY    DEFAULT ARRAY[],
    version               INTEGER       NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   DEFAULT now(),
    updated_at            TIMESTAMPTZ   DEFAULT now()
);

-- 2. channels
CREATE TABLE channels (
    id                  BIGINT        PRIMARY KEY,
    title               VARCHAR(255)  NOT NULL,
    username            VARCHAR(255),
    description         TEXT,
    subscriber_count    INTEGER       DEFAULT 0,
    category            VARCHAR(100),
    price_per_post_nano BIGINT,
    is_active           BOOLEAN       DEFAULT TRUE,
    owner_id            BIGINT        NOT NULL REFERENCES users(id),
    version             INTEGER       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   DEFAULT now(),
    updated_at          TIMESTAMPTZ   DEFAULT now()
);

-- 3. channel_memberships
CREATE TABLE channel_memberships (
    id              BIGSERIAL     PRIMARY KEY,
    channel_id      BIGINT        NOT NULL REFERENCES channels(id),
    user_id         BIGINT        NOT NULL REFERENCES users(id),
    role            VARCHAR(20)   NOT NULL CHECK (role IN ('OWNER', 'MANAGER')),
    rights          JSONB         DEFAULT '{}',
    invited_by      BIGINT        REFERENCES users(id),
    version         INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   DEFAULT now(),
    UNIQUE (channel_id, user_id)
);

-- 4. channel_pricing_rules
CREATE TABLE channel_pricing_rules (
    id              BIGSERIAL     PRIMARY KEY,
    channel_id      BIGINT        NOT NULL REFERENCES channels(id),
    name            VARCHAR(100)  NOT NULL,
    description     TEXT,
    post_type       VARCHAR(50)   NOT NULL,
    price_nano      BIGINT        NOT NULL CHECK (price_nano > 0),
    is_active       BOOLEAN       DEFAULT TRUE,
    sort_order      INTEGER       DEFAULT 0,
    version         INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   DEFAULT now(),
    updated_at      TIMESTAMPTZ   DEFAULT now()
);

-- 5. commission_tiers
CREATE TABLE commission_tiers (
    id              SERIAL        PRIMARY KEY,
    min_amount_nano BIGINT        NOT NULL DEFAULT 0,
    max_amount_nano BIGINT,
    rate_bp         INTEGER       NOT NULL CHECK (rate_bp >= 0 AND rate_bp <= 5000),
    description     VARCHAR(200),
    version         INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   DEFAULT now(),
    CHECK (max_amount_nano IS NULL OR max_amount_nano > min_amount_nano)
);

-- 6. deals
CREATE TABLE deals (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id          BIGINT        NOT NULL REFERENCES channels(id),
    advertiser_id       BIGINT        NOT NULL REFERENCES users(id),
    owner_id            BIGINT        NOT NULL REFERENCES users(id),
    pricing_rule_id     BIGINT        REFERENCES channel_pricing_rules(id),
    status              VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    amount_nano         BIGINT        NOT NULL CHECK (amount_nano > 0),
    commission_rate_bp  INTEGER       NOT NULL DEFAULT 1000,
    commission_nano     BIGINT        NOT NULL CHECK (commission_nano >= 0),
    deposit_address     VARCHAR(100),
    subwallet_id        INTEGER,
    creative_brief      JSONB,
    creative_draft      JSONB,
    message_id          BIGINT,
    content_hash        VARCHAR(100),
    deadline_at         TIMESTAMPTZ,
    published_at        TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    cancellation_reason TEXT,
    version             INTEGER       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   DEFAULT now(),
    updated_at          TIMESTAMPTZ   DEFAULT now(),
    funded_at           TIMESTAMPTZ,
    deposit_tx_hash     VARCHAR(100),
    payout_tx_hash      VARCHAR(100),
    refunded_tx_hash    VARCHAR(100),
    CONSTRAINT uq_deals_subwallet_id UNIQUE (subwallet_id)
);

-- 7. deal_events (non-partitioned for codegen)
CREATE TABLE deal_events (
    id              BIGSERIAL     PRIMARY KEY,
    deal_id         UUID          NOT NULL,
    event_type      VARCHAR(50)   NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30),
    actor_id        BIGINT,
    actor_type      VARCHAR(20)   DEFAULT 'USER',
    payload         JSONB         DEFAULT '{}',
    created_at      TIMESTAMPTZ   DEFAULT now()
);

-- 8. ledger_entries (non-partitioned for codegen)
CREATE TABLE ledger_entries (
    id              BIGSERIAL     PRIMARY KEY,
    deal_id         UUID,
    account_id      VARCHAR(100)  NOT NULL,
    entry_type      VARCHAR(30)   NOT NULL,
    debit_nano      BIGINT        NOT NULL DEFAULT 0 CHECK (debit_nano >= 0),
    credit_nano     BIGINT        NOT NULL DEFAULT 0 CHECK (credit_nano >= 0),
    idempotency_key VARCHAR(200)  NOT NULL,
    created_at      TIMESTAMPTZ   DEFAULT now(),
    tx_ref          UUID          NOT NULL,
    description     VARCHAR(500),
    CHECK (debit_nano > 0 OR credit_nano > 0),
    CHECK (NOT (debit_nano > 0 AND credit_nano > 0))
);

-- 8a. ledger_idempotency_keys
CREATE TABLE ledger_idempotency_keys (
    idempotency_key VARCHAR(200) PRIMARY KEY,
    created_at      TIMESTAMPTZ  DEFAULT now()
);

-- 9. account_balances
CREATE TABLE account_balances (
    account_id      VARCHAR(100)  PRIMARY KEY,
    balance_nano    BIGINT        NOT NULL DEFAULT 0,
    last_entry_id   BIGINT        NOT NULL DEFAULT 0,
    version         INTEGER       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ   DEFAULT now()
);

-- 10. ton_transactions
CREATE TABLE ton_transactions (
    id              BIGSERIAL     PRIMARY KEY,
    deal_id         UUID          REFERENCES deals(id),
    tx_hash         VARCHAR(100)  UNIQUE,
    direction       VARCHAR(3)    NOT NULL CHECK (direction IN ('IN', 'OUT')),
    amount_nano     BIGINT        NOT NULL CHECK (amount_nano > 0),
    from_address    VARCHAR(100),
    to_address      VARCHAR(100),
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    confirmations   INTEGER       DEFAULT 0,
    subwallet_id    INTEGER,
    version         INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   DEFAULT now(),
    confirmed_at    TIMESTAMPTZ,
    tx_type         VARCHAR(20)   CHECK (tx_type IN ('DEPOSIT', 'PAYOUT', 'REFUND', 'OVERPAYMENT_REFUND')),
    fee_nano        BIGINT        DEFAULT 0
);

-- 11. disputes
CREATE TABLE disputes (
    id              BIGSERIAL     PRIMARY KEY,
    deal_id         UUID          NOT NULL REFERENCES deals(id) UNIQUE,
    opened_by       BIGINT        NOT NULL REFERENCES users(id),
    reason          VARCHAR(50)   NOT NULL,
    description     TEXT,
    status          VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    outcome         VARCHAR(30),
    resolved_by     BIGINT        REFERENCES users(id),
    resolved_at     TIMESTAMPTZ,
    version         INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   DEFAULT now()
);

-- 12. dispute_evidence
CREATE TABLE dispute_evidence (
    id              BIGSERIAL     PRIMARY KEY,
    dispute_id      BIGINT        NOT NULL REFERENCES disputes(id),
    submitted_by    BIGINT        NOT NULL REFERENCES users(id),
    evidence_type   VARCHAR(30)   NOT NULL,
    content         JSONB         NOT NULL,
    content_hash    VARCHAR(64),
    created_at      TIMESTAMPTZ   DEFAULT now()
);

-- 13. notification_outbox
CREATE TABLE notification_outbox (
    id              BIGSERIAL     PRIMARY KEY,
    deal_id         UUID,
    idempotency_key VARCHAR(200),
    topic           VARCHAR(100)  NOT NULL,
    partition_key   VARCHAR(100),
    payload         JSONB         NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    retry_count     INTEGER       DEFAULT 0,
    version         INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   DEFAULT now(),
    processed_at    TIMESTAMPTZ
);

-- 14. posting_checks (non-partitioned for codegen)
CREATE TABLE posting_checks (
    id              BIGSERIAL     PRIMARY KEY,
    deal_id         UUID          NOT NULL,
    check_number    INTEGER       NOT NULL,
    message_id      BIGINT,
    content_hash    VARCHAR(100),
    status          VARCHAR(20)   NOT NULL,
    checked_at      TIMESTAMPTZ   DEFAULT now()
);

-- 15. pii_store
CREATE TABLE pii_store (
    id              BIGSERIAL     PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id),
    field_name      VARCHAR(50)   NOT NULL,
    encrypted_value BYTEA         NOT NULL,
    key_version     INTEGER       NOT NULL DEFAULT 1,
    version         INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   DEFAULT now(),
    updated_at      TIMESTAMPTZ   DEFAULT now(),
    UNIQUE (user_id, field_name)
);

-- 16. audit_log
CREATE TABLE audit_log (
    id              BIGSERIAL     PRIMARY KEY,
    actor_id        BIGINT,
    action          VARCHAR(100)  NOT NULL,
    entity_type     VARCHAR(50)   NOT NULL,
    entity_id       VARCHAR(100)  NOT NULL,
    old_value       JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ   DEFAULT now(),
    tx_ref          UUID
);

-- Sequence for subwallet_id generation
CREATE SEQUENCE deal_subwallet_seq START 1;
