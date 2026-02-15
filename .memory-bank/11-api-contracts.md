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
| `PUT` | `/api/v1/profile/settings` | `advert-market-identity` | Update settings (currency, notifications) |
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
| `GET` | `/api/v1/deals` | `advert-market-deal` | Deal list (role-filtered, cursor pagination) |
| `POST` | `/api/v1/deals` | `advert-market-deal` | Deal creation |
| `GET` | `/api/v1/deals/{id}` | `advert-market-deal` | Deal detail |
| `POST` | `/api/v1/deals/{id}/transition` | `advert-market-deal` | State transitions |

## Planned API

| Method | Path | Purpose | Beads ID | Target module |
|---|---|---|---|---|
| `GET` | `/api/v1/deals/{id}/timeline` | Deal event timeline | `advert-market-av4.5` | `advert-market-deal` |
| `POST` | `/api/v1/deals/{id}/negotiate` | Counter-offer flow | `advert-market-av4.5` | `advert-market-deal` |
| `GET` | `/api/v1/deals/{id}/deposit` | Deposit info | `advert-market-av4.3` | `advert-market-deal` |
| `GET` | `/api/v1/deals/{id}/escrow` | Escrow status | `advert-market-av4.3` | `advert-market-deal` |
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
| `GET` | `/api/v1/creatives` | Creative library list | `advert-market-7lx` | `advert-market-marketplace` |
| `POST` | `/api/v1/creatives` | Create creative template | `advert-market-7lx` | `advert-market-marketplace` |
| `GET` | `/api/v1/creatives/{id}` | Creative template detail | `advert-market-7lx` | `advert-market-marketplace` |
| `PUT` | `/api/v1/creatives/{id}` | Update creative template | `advert-market-7lx` | `advert-market-marketplace` |
| `DELETE` | `/api/v1/creatives/{id}` | Delete creative template (soft) | `advert-market-7lx` | `advert-market-marketplace` |
| `GET` | `/api/v1/creatives/{id}/versions` | Creative template version history | `advert-market-7lx` | `advert-market-marketplace` |
| `GET` | `/api/v1/wallet/summary` | Wallet aggregates | `advert-market-av4.6` | `advert-market-financial` |
| `GET` | `/api/v1/wallet/transactions` | Wallet history | `advert-market-av4.6` | `advert-market-financial` |
| `GET` | `/api/v1/wallet/transactions/{txId}` | Wallet transaction detail | `advert-market-av4.6` | `advert-market-financial` |
| `POST` | `/api/v1/wallet/withdraw` | Withdrawal request | `advert-market-av4.6` | `advert-market-financial` |

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

### Profile Payload Shape

Backend profile payload does not include a separate `telegramId` field.
Frontend treats `telegramId` as optional and falls back to `id`.

### OpenAPI Sync Pipeline

- OpenAPI artifact is exported from backend endpoint `/v3/api-docs.yaml` via:
  - `./gradlew :advert-market-app:generateOpenApiDocs`
- Local generation requires non-empty env values for security/config validation:
  - `JWT_SECRET`
  - `INTERNAL_API_KEY`
  - `TELEGRAM_BOT_TOKEN`
  - `TELEGRAM_WEBHOOK_SECRET`
  - `APP_MARKETPLACE_CHANNEL_BOT_USER_ID`
- Frontend types are regenerated from `api/openapi.yml`:
  - `cd advert-market-frontend && npm run api:types`

## Related Docs

- [Frontend catalog page spec](./15-frontend-pages/02-catalog.md)
- [Channel search implementation](./14-implementation-specs/29-channel-search-impl.md)
- [Deal state machine](./06-deal-state-machine.md)
- [Module architecture](./15-module-architecture.md)
