# API Contracts

## Overview

The platform exposes two API surfaces: a **public REST API** for the Mini App and a **private internal API** for worker callbacks. All APIs follow consistent conventions.

## API Conventions

| Convention | Value |
|-----------|-------|
| **Base path** | `/api/v1` (public), `/internal/v1` (private) |
| **Format** | JSON |
| **Authentication** | Telegram initData HMAC (public), network-level (internal) |
| **Versioning** | URL path versioning (`/v1/`, `/v2/`) |
| **Naming** | kebab-case for URLs, camelCase for JSON fields |
| **Pagination** | Cursor-based (`?cursor=xxx&limit=20`) |
| **Errors** | RFC 7807 Problem Details (`application/problem+json`) |
| **Amounts** | Always in nanoTON (BIGINT) |
| **Timestamps** | ISO 8601 with timezone (`2025-01-15T10:30:00Z`) |
| **IDs** | UUID v4 for deals and entries, BIGINT for Telegram IDs |

## Public REST API

### Deals

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/api/v1/deals` | Create deal offer | Advertiser |
| `GET` | `/api/v1/deals` | List deals (filtered by role) | Any authenticated |
| `GET` | `/api/v1/deals/{id}` | Get deal details | Deal participant |
| `POST` | `/api/v1/deals/{id}/accept` | Accept offer | Channel Owner |
| `POST` | `/api/v1/deals/{id}/reject` | Reject offer | Channel Owner |
| `POST` | `/api/v1/deals/{id}/negotiate` | Counter-offer | Either party |
| `POST` | `/api/v1/deals/{id}/cancel` | Cancel deal | Depends on state |
| `GET` | `/api/v1/deals/{id}/timeline` | Get event timeline | Deal participant |
| `GET` | `/api/v1/deals/{id}/deposit` | Get deposit address/status | Advertiser |
| `GET` | `/api/v1/deals/{id}/escrow` | Get escrow status | Deal participant |

### Creative

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/api/v1/deals/{id}/brief` | Submit creative brief | Advertiser |
| `GET` | `/api/v1/deals/{id}/brief` | Get creative brief | Deal participant |
| `POST` | `/api/v1/deals/{id}/creative` | Submit creative draft | Owner/Admin |
| `GET` | `/api/v1/deals/{id}/creative` | Get current draft | Deal participant |
| `GET` | `/api/v1/deals/{id}/creative/history` | Draft version history | Deal participant |
| `POST` | `/api/v1/deals/{id}/creative/approve` | Approve creative | Advertiser |
| `POST` | `/api/v1/deals/{id}/creative/revision` | Request revision | Advertiser |
| `POST` | `/api/v1/deals/{id}/publish` | Trigger publication | Owner/Admin |
| `POST` | `/api/v1/deals/{id}/schedule` | Schedule publication | Owner/Admin |

### Disputes

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/api/v1/deals/{id}/dispute` | Open dispute | Deal participant |
| `POST` | `/api/v1/deals/{id}/dispute/evidence` | Submit evidence | Deal participant |
| `GET` | `/api/v1/deals/{id}/dispute` | Get dispute status | Deal participant |
| `POST` | `/api/v1/deals/{id}/dispute/resolve` | Resolve dispute | Operator |

### Channels

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/channels` | Search/list channels | Any authenticated |
| `GET` | `/api/v1/channels/{id}` | Get channel details | Any authenticated |
| `POST` | `/api/v1/channels` | Register channel | Channel Owner |
| `PUT` | `/api/v1/channels/{id}` | Update listing | Owner/Admin |
| `DELETE` | `/api/v1/channels/{id}` | Deactivate listing | Owner |

### Team Management

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/channels/{id}/team` | List team members | Owner/Admin |
| `POST` | `/api/v1/channels/{id}/team` | Invite member | Owner |
| `PUT` | `/api/v1/channels/{id}/team/{userId}` | Update rights | Owner |
| `DELETE` | `/api/v1/channels/{id}/team/{userId}` | Remove member | Owner |

### Auth

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/api/v1/auth/validate` | Validate initData, get session | Telegram initData |

## Internal API (Worker Callbacks)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/internal/v1/worker-events` | Worker result callback | Network-level |

### Worker Event Payload

```json
{
  "event_type": "DEPOSIT_CONFIRMED",
  "deal_id": "550e8400-e29b-41d4-a716-446655440000",
  "idempotency_key": "dep-550e8400-tx-abc123",
  "payload": {
    "tx_hash": "abc123...",
    "amount_nano": 1000000000000,
    "confirmations": 3
  },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### Event Types

| Event Type | Source Worker |
|-----------|-------------|
| `DEPOSIT_CONFIRMED` | TON Deposit Watcher |
| `PAYOUT_COMPLETED` | Payout Executor |
| `REFUND_COMPLETED` | Refund Executor |
| `PUBLICATION_RESULT` | Post Scheduler |
| `VERIFICATION_RESULT` | Delivery Verifier |
| `RECONCILIATION_START` | Reconciliation Worker |

## Error Response Format

RFC 7807 Problem Details:

```json
{
  "type": "https://api.admarketplace.com/errors/deal-invalid-transition",
  "title": "Invalid State Transition",
  "status": 409,
  "detail": "Cannot transition deal from DRAFT to FUNDED",
  "instance": "/api/v1/deals/550e8400.../accept",
  "dealId": "550e8400-e29b-41d4-a716-446655440000",
  "currentStatus": "DRAFT",
  "requestedTransition": "FUNDED"
}
```

## Request/Response Schemas

### POST /api/v1/deals — Create Deal

**Request:**
```json
{
  "channelId": 123456789,
  "pricingRuleId": 42,
  "message": "Хочу разместить рекламу криптопроекта"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `channelId` | Long | Yes | Must exist, `is_active = true` |
| `pricingRuleId` | Long | Yes | Must belong to channel, `is_active = true` |
| `message` | String | No | Max 2000 chars |

**Response (201):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "DRAFT",
  "channelId": 123456789,
  "channelTitle": "Crypto News",
  "amountNano": 50000000000,
  "createdAt": "2025-01-15T10:30:00Z"
}
```

### POST /api/v1/deals/{id}/negotiate — Counter-Offer

**Request:**
```json
{
  "proposedAmountNano": 40000000000,
  "pricingRuleId": 42,
  "message": "Предлагаю цену со скидкой"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `proposedAmountNano` | Long | Yes | > 0 |
| `pricingRuleId` | Long | No | Change post type |
| `message` | String | No | Max 2000 chars |

### POST /api/v1/deals/{id}/creative — Submit Creative

**Request:**
```json
{
  "text": "Текст рекламного поста...",
  "mediaUrls": ["https://example.com/image.png"],
  "buttons": [
    {"text": "Перейти", "url": "https://example.com"}
  ],
  "format": "STANDARD"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `text` | String | Yes | Max 4096 chars (Telegram limit) |
| `mediaUrls` | String[] | No | Max 10 items, valid URLs |
| `buttons` | Object[] | No | Max 3 rows x 3 buttons |
| `format` | String | No | STANDARD, PINNED, STORY, REPOST, NATIVE |

### POST /api/v1/deals/{id}/dispute — Open Dispute

**Request:**
```json
{
  "reason": "POST_DELETED",
  "description": "Пост был удалён через 2 часа после публикации"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `reason` | String | Yes | Enum: POST_DELETED, POST_EDITED, WRONG_CONTENT, QUALITY_ISSUE, OTHER |
| `description` | String | Yes | Max 5000 chars |

### POST /api/v1/deals/{id}/dispute/evidence — Submit Evidence

**Request:**
```json
{
  "evidenceType": "SCREENSHOT",
  "content": {
    "url": "https://...",
    "caption": "Скриншот удалённого поста"
  }
}
```

### POST /api/v1/deals/{id}/schedule — Schedule Publication

**Request:**
```json
{
  "scheduledAt": "2025-01-16T14:00:00Z"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `scheduledAt` | ISO 8601 | Yes | Must be in the future, max 30 days ahead |

### POST /api/v1/channels — Register Channel

**Request:**
```json
{
  "channelId": 123456789,
  "description": "Крупнейший канал о криптовалютах",
  "topic": "crypto",
  "pricingRules": [
    {"name": "Стандартный пост", "postType": "STANDARD", "priceNano": 50000000000}
  ]
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `channelId` | Long | Yes | Bot must be admin in channel |
| `description` | String | No | Max 5000 chars |
| `topic` | String | Yes | Enum from predefined list |
| `pricingRules` | Object[] | Yes | Min 1 rule |

### POST /api/v1/auth/validate — Authenticate

**Request:**
```json
{
  "initData": "query_id=AAHdF6Iq...&auth_date=1234567890&hash=abc123..."
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 86400,
  "user": {
    "id": 123456789,
    "username": "johndoe",
    "displayName": "John Doe"
  }
}
```

---

## Cursor Pagination Algorithm

### Cursor Format

Opaque Base64-encoded JSON:

```json
{"v": 1, "sk": "<sort_key_value>", "id": "<tie_breaker_id>"}
```

### Pagination Response Envelope

```json
{
  "items": [...],
  "nextCursor": "eyJ2IjoxLC...",
  "hasNext": true
}
```

| Field | Type | Description |
|-------|------|-------------|
| `items` | Array | Current page results |
| `nextCursor` | String/null | Cursor for next page (null if last page) |
| `hasNext` | Boolean | Whether more results exist |

### Algorithm

1. Client sends `GET /api/v1/deals?cursor=xxx&limit=20`
2. Server decodes cursor -> extracts sort key + tie-breaker
3. Query adds WHERE clause: `(sort_key, id) > (cursor_sk, cursor_id)` (for ASC)
4. Fetch `limit + 1` rows
5. If fetched `limit + 1`: `hasNext = true`, encode last item as `nextCursor`, return first `limit`
6. If fetched <= `limit`: `hasNext = false`, `nextCursor = null`

### Default Limits

| Endpoint | Default Limit | Max Limit |
|----------|--------------|-----------|
| Deals list | 20 | 50 |
| Channel search | 20 | 50 |
| Deal timeline | 50 | 100 |
| Creative history | 10 | 20 |
| Team members | 20 | 50 |

---

## Related Documents

- [Backend API Components](./04-architecture/03-backend-api-components.md) — service implementation
- [Deal Lifecycle](./03-feature-specs/02-deal-lifecycle.md) — deal endpoints context
- [Workers](./04-architecture/04-workers.md) — worker callback pattern
- [Security & Compliance](./10-security-and-compliance.md) — auth details
- [Channel Search Implementation](./14-implementation-specs/29-channel-search-impl.md)
- [Creative JSONB Schemas](./14-implementation-specs/20-creative-jsonb-schemas.md)
