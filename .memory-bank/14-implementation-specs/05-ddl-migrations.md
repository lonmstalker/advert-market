# Complete DDL Migration Scripts

## Migration Tool: Liquibase

**Decision**: Liquibase for database migrations.

| Criterion | Liquibase | Flyway |
|-----------|-----------|--------|
| Format | YAML/XML/SQL changelogs | Pure SQL |
| Rollback | Auto-generated rollback support | Manual undo scripts |
| Diff/snapshot | Built-in DB diff and snapshot | No |
| Preconditions | Conditional execution | No |
| Java integration | Spring Boot auto-config | Spring Boot auto-config |

### Gradle

```groovy
dependencies {
    implementation 'org.liquibase:liquibase-core'
}
```

### Configuration

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

---

### Changelog Structure

```
src/main/resources/db/changelog/
  db.changelog-master.yaml          # Master changelog (includeAll)
  changes/
    001-init-schema.sql             # All tables + indexes
    002-triggers.sql                # Immutability + updated_at triggers
    003-partitions.sql              # Monthly partitions (2026)
    004-seed-data.sql               # Default commission tiers
    005-comments.sql                # COMMENT ON TABLE/COLUMN/FUNCTION
```

Master changelog:
```yaml
databaseChangeLog:
  - includeAll:
      path: db/changelog/changes/
      relativeToChangelogFile: false
```

---

## 001-init-schema.sql — All Tables

### 1. users

```sql
CREATE TABLE users (
    id              BIGINT        PRIMARY KEY,
    username        VARCHAR(255),
    first_name      VARCHAR(255)  NOT NULL,
    last_name       VARCHAR(255),
    language_code   VARCHAR(5)    DEFAULT 'ru',
    is_operator     BOOLEAN       DEFAULT FALSE,
    version         INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   DEFAULT now(),
    updated_at      TIMESTAMPTZ   DEFAULT now()
);
```

### 2. channels

```sql
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

CREATE INDEX idx_channels_owner ON channels(owner_id);
CREATE INDEX idx_channels_category ON channels(category) WHERE is_active;
CREATE INDEX idx_channels_active ON channels(is_active, subscriber_count DESC);
```

### 3. channel_memberships

```sql
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

CREATE INDEX idx_memberships_user ON channel_memberships(user_id);
```

### 4. channel_pricing_rules

```sql
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

CREATE INDEX idx_pricing_rules_channel ON channel_pricing_rules(channel_id, is_active);
```

### 5. commission_tiers

```sql
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
```

### 6. deals

```sql
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
    updated_at          TIMESTAMPTZ   DEFAULT now()
);

CREATE INDEX idx_deals_advertiser ON deals(advertiser_id, status);
CREATE INDEX idx_deals_owner ON deals(owner_id, status);
CREATE INDEX idx_deals_channel ON deals(channel_id);
CREATE INDEX idx_deals_status ON deals(status) WHERE status NOT IN ('COMPLETED_RELEASED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'EXPIRED');
CREATE INDEX idx_deals_deadline ON deals(deadline_at) WHERE deadline_at IS NOT NULL AND status NOT IN ('COMPLETED_RELEASED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'EXPIRED');
```

### 7. deal_events (partitioned, immutable)

```sql
CREATE TABLE deal_events (
    id              BIGSERIAL,
    deal_id         UUID          NOT NULL,
    event_type      VARCHAR(50)   NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30),
    actor_id        BIGINT,
    actor_type      VARCHAR(20)   DEFAULT 'USER',
    payload         JSONB         DEFAULT '{}',
    created_at      TIMESTAMPTZ   DEFAULT now(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE INDEX idx_deal_events_deal ON deal_events(deal_id, created_at DESC);
```

### 8. ledger_entries (partitioned, immutable, double-entry)

```sql
CREATE TABLE ledger_entries (
    id              BIGSERIAL,
    deal_id         UUID,
    account_id      VARCHAR(100)  NOT NULL,
    entry_type      VARCHAR(30)   NOT NULL,
    debit_nano      BIGINT        NOT NULL DEFAULT 0 CHECK (debit_nano >= 0),
    credit_nano     BIGINT        NOT NULL DEFAULT 0 CHECK (credit_nano >= 0),
    idempotency_key VARCHAR(200)  NOT NULL,
    created_at      TIMESTAMPTZ   DEFAULT now(),
    PRIMARY KEY (id, created_at),
    CHECK (debit_nano > 0 OR credit_nano > 0),
    CHECK (NOT (debit_nano > 0 AND credit_nano > 0))
) PARTITION BY RANGE (created_at);

CREATE UNIQUE INDEX idx_ledger_idempotency ON ledger_entries(idempotency_key, created_at);
CREATE INDEX idx_ledger_account ON ledger_entries(account_id, created_at DESC);
CREATE INDEX idx_ledger_deal ON ledger_entries(deal_id) WHERE deal_id IS NOT NULL;
```

**Note**: `idx_ledger_idempotency` includes `created_at` because partitioned tables require partition key in unique indexes.

### 9. account_balances (CQRS projection)

```sql
CREATE TABLE account_balances (
    account_id      VARCHAR(100)  PRIMARY KEY,
    balance_nano    BIGINT        NOT NULL DEFAULT 0 CHECK (balance_nano >= 0),
    last_entry_id   BIGINT        NOT NULL DEFAULT 0,
    version         INTEGER       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ   DEFAULT now()
);
```

### 10. ton_transactions

```sql
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
    confirmed_at    TIMESTAMPTZ
);

CREATE INDEX idx_ton_tx_deal ON ton_transactions(deal_id);
CREATE INDEX idx_ton_tx_pending ON ton_transactions(status) WHERE status = 'PENDING';
CREATE INDEX idx_ton_tx_address ON ton_transactions(to_address) WHERE direction = 'IN';
```

### 11. disputes

```sql
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

CREATE INDEX idx_disputes_status ON disputes(status) WHERE status = 'OPEN';
```

### 12. dispute_evidence (immutable)

```sql
CREATE TABLE dispute_evidence (
    id              BIGSERIAL     PRIMARY KEY,
    dispute_id      BIGINT        NOT NULL REFERENCES disputes(id),
    submitted_by    BIGINT        NOT NULL REFERENCES users(id),
    evidence_type   VARCHAR(30)   NOT NULL,
    content         JSONB         NOT NULL,
    created_at      TIMESTAMPTZ   DEFAULT now()
);

CREATE INDEX idx_evidence_dispute ON dispute_evidence(dispute_id);
```

### 13. notification_outbox

```sql
CREATE TABLE notification_outbox (
    id              BIGSERIAL     PRIMARY KEY,
    deal_id         UUID,
    recipient_id    BIGINT        NOT NULL,
    template        VARCHAR(50)   NOT NULL,
    payload         JSONB         NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    retry_count     INTEGER       DEFAULT 0,
    version         INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   DEFAULT now(),
    processed_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pending ON notification_outbox(created_at) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_recipient ON notification_outbox(recipient_id);
```

### 14. posting_checks (partitioned, write-once)

```sql
CREATE TABLE posting_checks (
    id              BIGSERIAL,
    deal_id         UUID          NOT NULL,
    check_number    INTEGER       NOT NULL,
    message_id      BIGINT,
    content_hash    VARCHAR(100),
    status          VARCHAR(20)   NOT NULL,
    checked_at      TIMESTAMPTZ   DEFAULT now(),
    PRIMARY KEY (id, checked_at)
) PARTITION BY RANGE (checked_at);

CREATE INDEX idx_posting_checks_deal ON posting_checks(deal_id, check_number);
```

### 15. pii_store

```sql
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

CREATE INDEX idx_pii_user ON pii_store(user_id);
```

### 16. audit_log (immutable)

```sql
CREATE TABLE audit_log (
    id              BIGSERIAL     PRIMARY KEY,
    actor_id        BIGINT,
    action          VARCHAR(100)  NOT NULL,
    entity_type     VARCHAR(50)   NOT NULL,
    entity_id       VARCHAR(100)  NOT NULL,
    old_value       JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ   DEFAULT now()
);

CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id, created_at DESC);
```

---

## Optimistic Locking (version column)

| Table | version | Reason |
|-------|:---:|--------|
| users | yes | Profile updates |
| channels | yes | Settings, price_per_post_nano |
| channel_memberships | yes | Role/rights changes |
| channel_pricing_rules | yes | Price changes |
| commission_tiers | yes | Admin configuration |
| deals | yes | FSM transitions (critical) |
| account_balances | yes | CQRS projection, concurrent updates |
| ton_transactions | yes | Confirmations, status |
| disputes | yes | Resolution process |
| notification_outbox | yes | Concurrent worker processing |
| pii_store | yes | Encrypted data updates |
| deal_events | no | Append-only, immutable |
| ledger_entries | no | Append-only, immutable |
| audit_log | no | Append-only, immutable |
| dispute_evidence | no | Append-only, immutable |
| posting_checks | no | Write-once checks |

`version` is managed by the application (jOOQ optimistic locking), NOT by trigger.

---

## Partition Creation (003-partitions.sql)

Monthly partitions for 2026 for: `deal_events`, `ledger_entries`, `posting_checks`.

```sql
CREATE TABLE deal_events_2026_01 PARTITION OF deal_events FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
-- ... 12 months each
```

**Automation**: Create pg_cron job or separate Liquibase changeset to add future partitions.

---

## Immutability Triggers (002-triggers.sql)

For append-only tables: `ledger_entries`, `deal_events`, `audit_log`, `dispute_evidence`.

```sql
CREATE OR REPLACE FUNCTION prevent_update_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'UPDATE and DELETE are not allowed on %', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;
```

---

## updated_at Auto-Update (002-triggers.sql)

```sql
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

Applied to: `users`, `channels`, `deals`.

---

## 005-comments.sql — COMMENT ON

All COMMENT ON TABLE, COMMENT ON COLUMN, and COMMENT ON FUNCTION statements. Comments in English. Covers:
- 16 tables
- Key columns (financial amounts, double-entry, versioning, CQRS, commission, PII)
- 2 trigger functions

---

## Related Documents

- [Data Stores](../04-architecture/05-data-stores.md)
- [Double-Entry Ledger](../05-patterns-and-decisions/05-double-entry-ledger.md)
- [Project Scaffold](./06-project-scaffold.md)
