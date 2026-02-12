# Auth Flow: initData HMAC + Session

## Overview

Authentication: Telegram Mini App `initData` -> HMAC-SHA256 validation -> JWT session.
Authorization: ABAC -- no fixed roles, permissions from resource relationships.

---

## 1. initData HMAC-SHA256 Validation

### Algorithm

1. Parse `initData` query string into key-value pairs
2. Remove `hash` parameter, store its value
3. Sort remaining pairs alphabetically by key
4. Join as `key=value\n` (newline-separated)
5. Compute secret: `HMAC-SHA256("WebAppData", bot_token)`
6. Compute hash: `HMAC-SHA256(secret, data_check_string)`
7. Compare computed hash with received `hash` (hex, constant-time)
8. Validate `auth_date` within anti-replay window (5 min)

### Implementation Notes

- Use `javax.crypto.Mac` with `HmacSHA256` algorithm
- Secret key: `HMAC-SHA256("WebAppData".getBytes(), botToken.getBytes())`
- Anti-replay window: configurable via `auth.anti-replay-window` (default 300 sec)
- Parse `user` JSON field via Jackson `ObjectMapper`
- Return `TelegramUser` record: `id`, `firstName`, `lastName`, `username`

---

## 2. Session Management: JWT + Redis Blacklist

**Decision**: Stateless JWT with Redis blacklist for revocation.

| Aspect | Value |
|--------|-------|
| Token type | JWT (HS256) |
| Claims | `sub` (user_id), `iat`, `exp`, `jti`, `is_operator` |
| Expiry | 24 hours |
| Refresh | Re-validate initData -> new JWT |
| Revocation | Redis blacklist by `jti` |
| Storage | Client-side (Authorization: Bearer header) |

### JWT Claims Structure

```json
{
  "sub": "123456789",
  "iat": 1700000000,
  "exp": 1700086400,
  "jti": "uuid-v4",
  "is_operator": false
}
```

### Token Creation

- Use `io.jsonwebtoken:jjwt-api` (JJWT library)
- Key: `Keys.hmacShaKeyFor(secret.getBytes())` (>= 32 bytes)
- Claims: `sub` = userId, `jti` = UUID.randomUUID(), `is_operator` flag
- Expiry: configurable via `auth.jwt.expiry` (default 24h)

---

## 3. Spring Security Filter Chain

### Configuration

- CSRF disabled (stateless API)
- Session: STATELESS
- Public endpoints: `/api/v1/auth/**`, `/api/v1/bot/webhook`, `/internal/v1/**`, `/actuator/health`
- Operator-only: `/actuator/**`
- Authenticated: `/api/v1/**`
- Custom `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`

### JWT Filter Logic

1. Extract `Authorization: Bearer <token>` header
2. Parse and validate JWT via `JwtService.parseToken(token)`
3. Check Redis blacklist by `jti`
4. If valid: set `TelegramAuthentication` in `SecurityContextHolder`
5. If invalid/blacklisted: return 401

---

## 4. Login Endpoint

```
POST /api/v1/auth/login
Content-Type: application/json
Body: { "initData": "<telegram_init_data_string>" }

-> 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600,
  "user": {
    "id": 123456789,
    "username": "johndoe",
    "displayName": "John Doe"
  }
}
```

### Profile Endpoints

```
GET /api/v1/profile
Authorization: Bearer <token>

-> 200 OK
{
  "id": 123456789,
  "telegramId": 123456789,
  "username": "johndoe",
  "displayName": "John Doe",
  "languageCode": "en",
  "onboardingCompleted": false,
  "interests": [],
  "createdAt": "2026-01-01T00:00:00Z"
}
```

```
PUT /api/v1/profile/onboarding
Authorization: Bearer <token>
Content-Type: application/json
Body: { "interests": ["tech", "gaming"] }

-> 200 OK (same schema as GET /profile, onboardingCompleted=true)
```

**User auto-registration**: on first login, upsert into `users` table.

---

## 5. ABAC Policy Enforcement

### Authorization Services

Three Spring `@Component` beans for authorization checks:

**DealAuthorizationService** (`@dealAuth`):
- `isParticipant(dealId)` -- userId == deal.advertiserId || userId == deal.ownerId
- `isAdvertiser(dealId)` -- userId == deal.advertiserId
- `channelId(dealId)` -- returns deal's channel_id for chained checks

**ChannelAuthorizationService** (`@channelAuth`):
- `isOwner(channelId)` -- membership.role == "OWNER"
- `hasRight(channelId, right)` -- role == "OWNER" or rights.get(right) == true

**AuthorizationService** (`@auth`):
- `isOperator()` -- userContext.isOperator

### @PreAuthorize Mapping (Key Endpoints)

```
POST   /api/v1/deals                   -> @PreAuthorize("isAuthenticated()")
GET    /api/v1/deals/{id}              -> @PreAuthorize("@dealAuth.isParticipant(#id)")
POST   /api/v1/deals/{id}/accept       -> @PreAuthorize("@channelAuth.hasRight(@dealAuth.channelId(#id), 'moderate')")
POST   /api/v1/deals/{id}/creative     -> @PreAuthorize("@channelAuth.hasRight(@dealAuth.channelId(#id), 'moderate')")
POST   /api/v1/deals/{id}/approve      -> @PreAuthorize("@dealAuth.isAdvertiser(#id)")
POST   /api/v1/deals/{id}/publish      -> @PreAuthorize("@channelAuth.hasRight(@dealAuth.channelId(#id), 'publish')")
POST   /api/v1/deals/{id}/dispute      -> @PreAuthorize("@dealAuth.isParticipant(#id)")
POST   /api/v1/deals/{id}/dispute/resolve -> @PreAuthorize("@auth.isOperator()")
POST   /api/v1/channels/{id}/team      -> @PreAuthorize("@channelAuth.hasRight(#id, 'manage_team')")
```

### Request-Scoped Attribute Cache

`UserAttributeContext` is `@RequestScope`:
- Holds `userId`, `isOperator`
- Caches `ChannelMembership` lookups per channelId within request
- Populated by JWT filter from token claims

---

## 6. Redis Token Blacklist

```
Key: token:blacklist:{jti}
Value: "1"
TTL: remaining JWT lifetime
```

- `blacklist(jti, expiresAt)` -- SET with TTL = Duration.between(now, expiresAt)
- `isBlacklisted(jti)` -- EXISTS check

---

## 7. Configuration

```yaml
auth:
  jwt:
    secret: ${JWT_SECRET}
    expiry: 24h
  anti-replay-window: 300  # seconds
telegram:
  bot:
    token: ${TELEGRAM_BOT_TOKEN}
```

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | >= 32 byte random string for HS256 |
| `TELEGRAM_BOT_TOKEN` | Bot token for HMAC key derivation |

---

## Related Documents

- [ABAC Pattern](../05-patterns-and-decisions/08-abac.md)
- [Security & Compliance](../10-security-and-compliance.md)
- [ABAC Spring Security](./13-abac-spring-security.md)