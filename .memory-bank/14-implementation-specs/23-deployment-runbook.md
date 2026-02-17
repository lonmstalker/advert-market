# Deployment Runbook Contract (Summary)

## Scope

This document is a contract-level summary only.
Runtime deployment truth is maintained in:

- `deploy/RUNBOOK.md` (operational procedure, blue-green/canary, rollback)
- `deploy/README.md` (server preparation, env variable matrix)
- `deploy/docker-compose.prod.yml` (effective runtime wiring and defaults)

If this file disagrees with those files, `deploy/*` is canonical.

## Canonical Runtime Baseline

- Domain: `teleinsight.in` (and `www.teleinsight.in`)
- Topology: Docker Compose production with blue-green app containers behind nginx
- Shared dependencies: PostgreSQL, Redis, Kafka, MinIO
- API/webhook traffic enters via nginx and is routed to active color

## Environment Contract

Do not duplicate full variable tables here.
Use `deploy/README.md` and `deploy/RUNBOOK.md` as the source of truth for required/optional variables.

Critical required secrets include:

- `DB_PASSWORD`, `JWT_SECRET`
- `TELEGRAM_BOT_TOKEN`, `TELEGRAM_WEBHOOK_SECRET`, `TELEGRAM_WEBHOOK_URL`, `TELEGRAM_WEBAPP_URL`
- `INTERNAL_API_KEY`, `CANARY_ADMIN_TOKEN`
- `TON_API_KEY`, `TON_WALLET_MNEMONIC`, `PII_ENCRYPTION_KEY`
- `APP_MARKETPLACE_CHANNEL_BOT_USER_ID`
- `CREATIVES_STORAGE_ACCESS_KEY`, `CREATIVES_STORAGE_SECRET_KEY`

Representative optional variables (see deploy docs for full list and defaults):

- `TON_NETWORK`
- `APP_TELEGRAM_WELCOME_CUSTOM_EMOJI_ID`
- `CREATIVES_STORAGE_ENABLED`, `CREATIVES_STORAGE_BUCKET`, `CREATIVES_STORAGE_REGION`
- `CREATIVES_STORAGE_PUBLIC_BASE_URL`, `CREATIVES_STORAGE_KEY_PREFIX`
- `APP_IMAGE`

## Operational Flow

Use `deploy/RUNBOOK.md` for all execution steps:

1. Initial deploy
2. Blue-green rollout
3. Canary ramp-up/rollback
4. Instant rollback by color switch
5. Health checks and readiness/liveness verification

## Drift Policy

- This file stays intentionally brief to reduce duplication.
- Any deployment behavior/env change must be reflected in `deploy/*` first.
- Update this summary only when the canonical deployment model itself changes.
