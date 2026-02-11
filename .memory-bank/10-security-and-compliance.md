# Security and Compliance

## Overview

The platform handles other people's money and personal data. Security is not optional — it's a core requirement at every layer.

## Authentication

### Telegram initData Verification

All user requests are authenticated via Telegram Mini App `initData`:

1. Mini App sends `initData` string with every API request
2. Auth Service validates HMAC-SHA256 signature using bot token
3. Anti-replay check: `auth_date` must be within acceptable window
4. Extract `user_id`, `first_name`, `username` from validated data
5. Issue session token for subsequent requests

### Session Management

| Aspect | Implementation |
|--------|---------------|
| **Token type** | JWT or opaque session token |
| **Storage** | Redis (server-side sessions) or stateless JWT |
| **Expiry** | Configurable (recommended 24h) |
| **Refresh** | On valid `initData` re-validation |

### Anti-Replay

- `auth_date` in initData is checked against server time
- Maximum allowed clock skew: configurable (default 5 minutes)
- Prevents replay of intercepted initData strings

## Authorization (ABAC)

The platform uses **Attribute-Based Access Control** (ABAC). There is no fixed `role` column on the `users` table — a single user can be an advertiser in one deal and a channel owner in another. Permissions are evaluated at runtime from subject, resource, action, and context attributes.

See [ABAC Pattern](./05-patterns-and-decisions/08-abac.md) for full policy rules.

### Policy Evaluation Flow

```
Request → Auth Service →
  1. Validate session token
  2. Extract user_id (subject)
  3. Load subject attributes:
     - users.is_operator
     - channel_memberships for target channel (if applicable)
  4. Load resource attributes:
     - deal.advertiser_id, deal.owner_id, deal.status, deal.amount_nano
  5. Evaluate ABAC policy for requested action
  6. Allow or deny (403 Forbidden)
```

### Endpoint Authorization

| Endpoint Category | ABAC Policy |
|-------------------|-------------|
| Channel browsing | Valid session (any authenticated user) |
| Deal operations | `subject.id == deal.advertiser_id` OR `subject.id == deal.owner_id` OR channel membership |
| Channel management | `membership(channel_id, user_id).role == 'OWNER'` OR `rights.manage_listings` |
| Team management | `membership.role == 'OWNER'` OR `rights.manage_team` |
| Operator endpoints | `subject.is_operator == true` |
| Internal worker callbacks | Internal network only (no user auth) |

## PII Protection

### PII Vault

Personally identifiable information is stored in an isolated `pii_store` with field-level encryption:

| Field | Encryption | Storage |
|-------|-----------|---------|
| `ton_payout_address` | AES-256-GCM | `pii_store` table |
| Telegram profile (`first_name`, `last_name`, `username`) | None (public Telegram data) | `users` table |

Telegram profile data (`first_name`, `last_name`, `username`) хранится в таблице `users` для отображения в UI. Это публичные данные Telegram-профиля. Чувствительные данные (адреса кошельков) хранятся только в зашифрованном `pii_store`.

### Encryption

- **Algorithm**: AES-256-GCM (authenticated encryption)
- **Key management**: Environment variable or external KMS (Scaled)
- **Key rotation**: Supported via key versioning in encrypted payload

## Financial Security

### Immutability

All financial records are append-only:

| Table | Protection |
|-------|-----------|
| `ledger_entries` | No UPDATE/DELETE, database triggers enforce |
| `audit_log` | WORM — Write Once Read Many |
| `deal_events` | No UPDATE/DELETE |
| `dispute_evidence` | No UPDATE/DELETE, `content_hash` (SHA-256) for tamper detection |

### Custodial Risk Mitigation

The platform holds custodial TON funds. Mitigations:

| Risk | Mitigation |
|------|-----------|
| Double payout | Redis distributed locks + `tx_hash` PK dedup |
| Missing funds | Three-way reconciliation (ledger vs TON vs deals) |
| Unauthorized release | State machine actor checks + operator review for high-value |
| Hot wallet compromise | Per-deal deposit addresses (no shared hot wallet in MVP) |

### Confirmation Policy

High-value deposits receive additional scrutiny:

| Amount | Confirmations | Review |
|--------|:---:|---|
| <= 100 TON | 1 | None |
| <= 1000 TON | 3 | None |
| > 1000 TON | 5 | Operator review required |

## Worker Security

### Internal-Only Endpoints

Worker Callback Controller (`POST /internal/v1/worker-events`) is tagged `#internal-only`:

- Not exposed to public internet
- Network-level access control (internal VPC/subnet)
- No user authentication (trusted internal service)

### Worker Isolation (Scaled)

In Scaled deployment, financial workers run on dedicated hosts:

- Blast radius isolation — a worker crash doesn't affect the API
- Separate monitoring and alerting
- Independent scaling

## Audit Trail

Every security-relevant operation is recorded:

| Event | Storage | Retention |
|-------|---------|-----------|
| Authentication attempts | Application logs | 90 days |
| Authorization failures | Application logs | 90 days |
| Financial operations | `audit_log` (WORM) | Indefinite |
| State transitions | `deal_events` | Indefinite |
| Dispute evidence | `dispute_evidence` | Indefinite |
| Reconciliation results | `audit_log` | Indefinite |

## Secret Management

### Secret Storage

| Secret | MVP Storage | Scaled Storage |
|--------|------------|---------------|
| Telegram Bot Token | Environment variable | HashiCorp Vault / AWS Secrets Manager |
| Database passwords | Environment variable | External KMS |
| PII encryption key | Environment variable | External KMS with auto-rotation |
| JWT signing key | Environment variable | External KMS |
| Webhook secret token | Environment variable | External KMS |
| TON wallet private key | Environment variable (encrypted) | HSM / Vault Transit |

### Secret Rotation

| Secret | Rotation Frequency | Procedure |
|--------|-------------------|-----------|
| PII encryption key | Annually or on compromise | Re-encrypt with new key_version, keep old key for decryption |
| JWT signing key | Quarterly | Blue-green: new key signs, both keys verify during grace period |
| Database passwords | Quarterly | Rolling update via deployment pipeline |
| Webhook secret | On compromise only | Re-register webhook with new secret |

### Access Control

- Secrets injected via `.env.server` file (gitignored, `chmod 600`)
- Only `ad-marketplace` system user has read access
- CI/CD secrets stored in GitHub Actions secrets (encrypted)
- No secrets in Docker images, Gradle files, or source code

## Data Retention & GDPR

### Data Retention Policy

| Data Category | Retention | Rationale |
|--------------|-----------|-----------|
| Financial records (`ledger_entries`, `audit_log`) | Indefinite | Legal/audit requirement |
| Deal events (`deal_events`) | Indefinite | Audit trail |
| User profiles (`users`) | Until account deletion | Active data |
| PII (`pii_store`) | Until account deletion + 30 day grace | Right to erasure |
| Posting checks (`posting_checks`) | 12 months | Dispute evidence window |
| Notification outbox | 90 days after delivery | Debugging |
| Application logs | 90 days | Operational |

### Right to Erasure (Account Deletion)

1. User requests deletion via Mini App settings
2. System checks for active deals (block if any `IN_PROGRESS`)
3. Pseudonymize `users` record: clear `username`, `display_name`, set `is_deleted = true`
4. Delete `pii_store` record (TON address)
5. Financial records preserved with pseudonymized user_id (legal requirement)
6. Deletion logged in `audit_log`

### Data Export

User can request data export (JSON) containing:
- Profile data, channel memberships, deal history (anonymized counterparty), notification history

## Input Validation

| Layer | Validation |
|-------|-----------|
| **Mini App** | Client-side form validation (UX only) |
| **Backend API** | Spring validation annotations, request sanitization |
| **Database** | Constraints, CHECK clauses, FK integrity |
| **Financial** | Amount > 0, balance >= 0, debit = credit invariant |

## Related Documents

- [Auth Service](./04-architecture/03-backend-api-components.md) — authentication implementation
- [Team Management](./03-feature-specs/07-team-management.md) — RBAC model
- [Confirmation Policy](./07-financial-system/06-confirmation-policy.md) — deposit security
- [Reconciliation](./07-financial-system/04-reconciliation.md) — financial safety net
- [Idempotency Strategy](./05-patterns-and-decisions/07-idempotency-strategy.md) — double-execution prevention
- [PII Encryption](./14-implementation-specs/07-pii-encryption.md) — encryption details
- [Logging Strategy](./14-implementation-specs/36-logging-strategy.md) — sensitive data redaction
