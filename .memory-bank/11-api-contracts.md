# API Contracts

## Overview

This document is the canonical API contract map for the current `HEAD` state.
It is intentionally split into two sections:

- `Implemented API (HEAD)`: endpoints that exist in backend controllers now.
- `Planned API`: target-state endpoints that are not implemented yet.

Rules:

- Code is the source of truth for runtime behavior.
- Planned endpoints must always reference a live Beads issue and target module.
- If an endpoint moves from planned to implemented, this file must be updated in the same change.

## Contract Conventions

| Convention | Value |
|---|---|
| Public base path | `/api/v1` |
| Internal base path | `/internal/v1` |
| Format | JSON |
| Auth | Bearer JWT + Telegram init-data bootstrap |
| Pagination | Cursor (`nextCursor`, `hasNext`) |
| Error format | RFC 7807 Problem Details |
| Amount unit | nanoTON (integer) |
| Timestamps | ISO-8601 UTC |

## Implemented API (HEAD)

### Identity

| Method | Path | Module | Notes |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | `advert-market-identity` | Telegram init-data login |
| `POST` | `/api/v1/auth/logout` | `advert-market-identity` | Session invalidation |
| `GET` | `/api/v1/profile` | `advert-market-identity` | User profile |
| `PUT` | `/api/v1/profile/onboarding` | `advert-market-identity` | Complete onboarding |
| `PUT` | `/api/v1/profile/language` | `advert-market-identity` | Update language |
| `PUT` | `/api/v1/profile/settings` | `advert-market-identity` | Update settings (`currencyMode`, `displayCurrency`, notifications) |
| `PUT` | `/api/v1/profile/wallet` | `advert-market-identity` | Update user TON wallet address |
| `DELETE` | `/api/v1/profile` | `advert-market-identity` | Delete account |

### Marketplace Catalog

| Method | Path | Module | Notes |
|---|---|---|---|
| `GET` | `/api/v1/categories` | `advert-market-marketplace` | Active categories |
| `GET` | `/api/v1/post-types` | `advert-market-marketplace` | Localized post types |
| `GET` | `/api/v1/channels` | `advert-market-marketplace` | Search/list channels |
| `GET` | `/api/v1/channels/my` | `advert-market-marketplace` | Channels owned by authenticated user |
| `GET` | `/api/v1/channels/count` | `advert-market-marketplace` | Count channels by filters |
| `GET` | `/api/v1/channels/{id}` | `advert-market-marketplace` | Channel detail |
| `POST` | `/api/v1/channels/verify` | `advert-market-marketplace` | Verify ownership/bot rights |
| `POST` | `/api/v1/channels` | `advert-market-marketplace` | Register channel |
| `PUT` | `/api/v1/channels/{id}` | `advert-market-marketplace` | Update channel |
| `DELETE` | `/api/v1/channels/{id}` | `advert-market-marketplace` | Deactivate channel |

### Team Management

| Method | Path | Module | Notes |
|---|---|---|---|
| `GET` | `/api/v1/channels/{channelId}/team` | `advert-market-marketplace` | Returns `List<TeamMemberDto>` |
| `POST` | `/api/v1/channels/{channelId}/team` | `advert-market-marketplace` | Invite manager |
| `PUT` | `/api/v1/channels/{channelId}/team/{userId}` | `advert-market-marketplace` | Update rights |
| `DELETE` | `/api/v1/channels/{channelId}/team/{userId}` | `advert-market-marketplace` | Remove member |

### Pricing Rules

| Method | Path | Module | Notes |
|---|---|---|---|
| `GET` | `/api/v1/channels/{channelId}/pricing` | `advert-market-marketplace` | List pricing rules |
| `POST` | `/api/v1/channels/{channelId}/pricing` | `advert-market-marketplace` | Create pricing rule |
| `PUT` | `/api/v1/channels/{channelId}/pricing/{ruleId}` | `advert-market-marketplace` | Update pricing rule |
| `DELETE` | `/api/v1/channels/{channelId}/pricing/{ruleId}` | `advert-market-marketplace` | Delete pricing rule |

### Creative Library

| Method | Path | Module | Notes |
|---|---|---|---|
| `GET` | `/api/v1/creatives` | `advert-market-marketplace` | List creative templates |
| `POST` | `/api/v1/creatives` | `advert-market-marketplace` | Create creative template |
| `GET` | `/api/v1/creatives/{id}` | `advert-market-marketplace` | Creative template detail |
| `PUT` | `/api/v1/creatives/{id}` | `advert-market-marketplace` | Update creative template |
| `DELETE` | `/api/v1/creatives/{id}` | `advert-market-marketplace` | Delete creative template |
| `GET` | `/api/v1/creatives/{id}/versions` | `advert-market-marketplace` | Creative template version history |
| `POST` | `/api/v1/creatives/media` | `advert-market-marketplace` | Upload creative media asset (`multipart/form-data`) |
| `DELETE` | `/api/v1/creatives/media/{mediaId}` | `advert-market-marketplace` | Delete creative media asset |

### Communication

| Method | Path | Module | Notes |
|---|---|---|---|
| `POST` | `/api/v1/bot/webhook` | `advert-market-communication` | Telegram webhook |

### Internal

| Method | Path | Module | Notes |
|---|---|---|---|
| `POST` | `/internal/v1/worker-events` | `advert-market-app` | Worker callbacks |
| `GET` | `/internal/v1/canary` | `advert-market-app` | Canary read config |
| `PUT` | `/internal/v1/canary` | `advert-market-app` | Canary update config |

### Deals

| Method | Path | Module | Notes |
|---|---|---|---|
| `GET` | `/api/v1/deals` | `advert-market-deal` | Deal list (cursor pagination, optional `status` filter) |
| `POST` | `/api/v1/deals` | `advert-market-deal` | Deal creation |
| `GET` | `/api/v1/deals/{id}` | `advert-market-deal` | Deal detail |
| `POST` | `/api/v1/deals/{id}/transition` | `advert-market-deal` | State transitions |
| `GET` | `/api/v1/deals/{id}/deposit` | `advert-market-deal` | Deposit address + confirmation progress |

### Wallet

| Method | Path | Module | Notes |
|---|---|---|---|
| `GET` | `/api/v1/wallet/summary` | `advert-market-financial` | Wallet aggregates |
| `GET` | `/api/v1/wallet/transactions` | `advert-market-financial` | Wallet history |
| `POST` | `/api/v1/wallet/withdraw` | `advert-market-financial` | Withdrawal request |

### Admin

| Method | Path | Module | Notes |
|---|---|---|---|
| `POST` | `/api/v1/admin/deposits/{id}/approve` | `advert-market-deal` | Manual operator approval for flagged deposits |
| `POST` | `/api/v1/admin/deposits/{id}/reject` | `advert-market-deal` | Manual operator rejection for flagged deposits |

#### Deal Contract Gate (2026-02-16)

- OpenAPI regenerated from backend `HEAD` confirms:
  - `GET /api/v1/deals`
  - `POST /api/v1/deals`
  - `GET /api/v1/deals/{id}`
  - `POST /api/v1/deals/{id}/transition`
  - `GET /api/v1/deals/{id}/deposit`
- Endpoints absent in generated contract:
  - `/api/v1/deals/{id}/timeline`
  - `/api/v1/deals/{id}/negotiate`
  - `/api/v1/deals/{id}/brief*`, `/creative*`, `/schedule*`, `/publish*`
- Frontend policy: deal creative sub-flow is blocked until backend contract exposes dedicated endpoints.

## Planned API

| Method | Path | Purpose | Beads ID | Target module |
|---|---|---|---|---|
| `POST` | `/api/v1/deals/{id}/brief` | Submit brief | `advert-market-6wx.3` | `advert-market-deal` |
| `GET` | `/api/v1/deals/{id}/brief` | Read brief | `advert-market-6wx.3` | `advert-market-deal` |
| `POST` | `/api/v1/deals/{id}/creative` | Submit creative draft | `advert-market-6wx.3` | `advert-market-deal` |
| `GET` | `/api/v1/deals/{id}/creative` | Read current draft | `advert-market-6wx.3` | `advert-market-deal` |
| `GET` | `/api/v1/deals/{id}/creative/history` | Draft revisions | `advert-market-6wx.3` | `advert-market-deal` |
| `POST` | `/api/v1/deals/{id}/creative/approve` | Approve creative | `advert-market-6wx.3` | `advert-market-deal` |
| `POST` | `/api/v1/deals/{id}/creative/revision` | Request revision | `advert-market-6wx.3` | `advert-market-deal` |
| `POST` | `/api/v1/deals/{id}/publish` | Publish command | `advert-market-6wx.3` | `advert-market-deal` |
| `POST` | `/api/v1/deals/{id}/schedule` | Schedule publish | `advert-market-6wx.3` | `advert-market-deal` |
| `POST` | `/api/v1/deals/{id}/dispute` | Open dispute | `advert-market-4fr.1` | `advert-market-deal` |
| `GET` | `/api/v1/deals/{id}/dispute` | Get dispute status | `advert-market-4fr.1` | `advert-market-deal` |
| `POST` | `/api/v1/deals/{id}/dispute/evidence` | Attach evidence | `advert-market-4fr.1` | `advert-market-deal` |
| `POST` | `/api/v1/deals/{id}/dispute/resolve` | Resolve dispute | `advert-market-4fr.1` | `advert-market-deal` |
| `GET` | `/api/v1/wallet/transactions/{txId}` | Wallet transaction detail | `advert-market-av4.6` | `advert-market-financial` |

## Compatibility Notes

### Channel Search Query Params

Current backend canonical query params:

- `query`, `category`, `minSubscribers`, `maxSubscribers`, `minPrice`, `maxPrice`, `minEngagement`, `language`, `sort`, `cursor`, `limit`

Backward-compatible aliases accepted by backend:

- `q -> query`
- `minSubs -> minSubscribers`
- `maxSubs -> maxSubscribers`
- `sort=subscribers -> SUBSCRIBERS_DESC`
- `sort=price_asc -> PRICE_ASC`
- `sort=price_desc -> PRICE_DESC`
- `sort=er -> ENGAGEMENT_DESC`

### Team Payload Shape

Backend returns raw list (`List<TeamMemberDto>`) with uppercase enums (`OWNER`, `MANAGER`, `MANAGE_TEAM`, ...).
Frontend normalizes both legacy `{members:[...]}` and list payload into a single internal shape.

Current rights enum includes:

- `MODERATE`
- `PUBLISH`
- `MANAGE_LISTINGS`
- `MANAGE_TEAM`
- `VIEW_STATS`

### Channel Manage ABAC Contract

- `PUT /api/v1/channels/{id}` and `DELETE /api/v1/channels/{id}` are protected by ABAC right `MANAGE_LISTINGS`.
- Access is allowed when:
  - membership role is `OWNER`, or
  - membership role is `MANAGER` and `rights.manage_listings == true`.
- The same ABAC gate is used for pricing write operations:
  - `POST /api/v1/channels/{channelId}/pricing`
  - `PUT /api/v1/channels/{channelId}/pricing/{ruleId}`
  - `DELETE /api/v1/channels/{channelId}/pricing/{ruleId}`

### Channel Owner Note Contract

- `ChannelUpdateRequest` supports `customRules` (owner note/free-form listing rules).
- `ChannelDetailResponse` returns `rules.customRules`.
- Persistence source of truth is `channels.custom_rules` (`TEXT NULL`).

### Profile Payload Shape

Backend profile payload does not include a separate `telegramId` field.
Frontend treats `telegramId` as optional and falls back to `id`.

Profile payload includes:

- `displayCurrency`: effective display currency (always resolved value for UI)
- `currencyMode`: `AUTO | MANUAL`

`PUT /api/v1/profile/settings` contract:

- `currencyMode=AUTO` can omit `displayCurrency` (server resolves by language mapping)
- `currencyMode=MANUAL` requires `displayCurrency`
- Backward compatibility: `displayCurrency` without `currencyMode` is treated as manual override (`MANUAL`)

### OpenAPI Sync Pipeline

- OpenAPI artifact is exported from backend endpoint `/v3/api-docs.yaml` via:
  - `./gradlew :advert-market-app:generateOpenApiDocs`
- Local generation requires non-empty env values for security/config validation:
  - `JWT_SECRET`
  - `INTERNAL_API_KEY`
  - `TELEGRAM_BOT_TOKEN`
  - `TELEGRAM_WEBHOOK_SECRET`
  - `APP_MARKETPLACE_CHANNEL_BOT_USER_ID`
  - `TON_API_KEY`
  - `TON_WALLET_MNEMONIC`
  - `PII_ENCRYPTION_KEY` (base64 for 32-byte key)
- If local PostgreSQL doesn't have `pg_search` extension, run docs generation with:
  - `SPRING_LIQUIBASE_ENABLED=false`
- Frontend types are regenerated from `api/openapi.yml`:
  - `cd advert-market-frontend && npm run api:types`

### Telegram Webhook Behavior Contract

For `POST /api/v1/bot/webhook`:

- Invalid webhook secret returns `401`.
- Malformed JSON payload returns `400`.
- Valid payload returns `200` and is processed asynchronously.
- Duplicate `update_id` must be deduplicated (no repeated side effects).
- `my_chat_member` transitions drive channel state sync:
  - admin -> left: channel deactivation + owner notification (`CHANNEL_BOT_REMOVED`)
  - admin -> member: channel deactivation + owner notification (`CHANNEL_BOT_DEMOTED`)
  - left/member -> administrator: channel reactivation + owner notification (`CHANNEL_BOT_RESTORED`)

## Related Docs

- [Frontend catalog page spec](./15-frontend-pages/02-catalog.md)
- [Channel search implementation](./14-implementation-specs/29-channel-search-impl.md)
- [Deal state machine](./06-deal-state-machine.md)
- [Module architecture](./15-module-architecture.md)
