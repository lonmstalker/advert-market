# Security and Compliance

## Overview

The platform is custodial for TON funds and must prioritize:

- auth replay resistance
- financial idempotency
- operator action control
- immutable auditability

## Authentication

### Telegram initData validation

Current baseline:

1. Verify Telegram HMAC signature.
2. Validate `auth_date` freshness.
3. Apply anti-replay nonce/hash check in Redis (with TTL).

Security posture:

- Anti-replay window: **30 seconds**.
- Replayed `initData` hash is rejected.

### Session and token revocation

- JWT revocation uses Redis-backed blacklist.
- Runtime behavior is fail-open with bounded local fallback cache for outages.
- Short token TTL and alerting are required to limit fail-open risk.

## Authorization

ABAC remains the authorization model for user-facing APIs.

For admin surface (`/api/v1/admin/*` target state):

- role partitioning: `L1_SUPPORT`, `L2_ARBITRATOR`, `TREASURY_MASTER`
- mandatory strong auth (TOTP/WebAuthn)
- maker-checker for high-value decisions

## Financial Security

### Outbound TON idempotency

Every outbound payout/refund must follow persisted lifecycle:

1. `CREATED` record persisted before blockchain send.
2. `SUBMITTED` with tx hash after successful `sendBoc`.
3. `CONFIRMED` after successful post-send processing.
4. `ABANDONED` on uncertain send failure requiring reconciliation.

Rules:

- No blind resend when an unresolved outbound record exists.
- Retry is allowed only after explicit reconciliation.

### Economic abuse controls

- Minimum deal amount: **0.5 TON** (anti-dust baseline).
- Withdrawal controls:
  - per-transaction manual-review threshold
  - 24h velocity limit per account

## Secrets and Key Management

Current code path still supports env-based keys for pre-prod operation.
Production target remains external secret management:

- KMS/Vault for decryption at runtime
- no mnemonic/plain keys in static config files

## Data Protection

- Sensitive payout address data stored in PII vault path.
- Encryption at rest with key versioning.
- No secrets or raw PII in logs.

## Audit and Immutability

- `ledger_entries`, `deal_events`, `audit_log` are append-oriented.
- Financial events are traceable end-to-end by deal and tx hash.

## Operational Controls

- Emergency halt capability for financial execution path (target state).
- Reconciliation checks required before/after risky maintenance.

## Related Docs

- [Deal state machine](./06-deal-state-machine.md)
- [Financial reconciliation](./07-financial-system/04-reconciliation.md)
- [Idempotency strategy](./05-patterns-and-decisions/07-idempotency-strategy.md)
