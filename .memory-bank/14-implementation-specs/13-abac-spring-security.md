# ABAC Policy Rules -- Spring Security Mapping

## Overview

No fixed user roles. Permissions determined by resource relationships at request time.

---

## Authorization Components

### 1. UserAttributeContext (@RequestScope)

Request-scoped bean populated by JWT filter:
- `userId` (long) -- from JWT `sub` claim
- `isOperator` (boolean) -- from JWT `is_operator` claim
- `getMembership(channelId)` -- lazy-loaded, cached per request

Caches `ChannelMembership` lookups to avoid repeated DB hits within a single request.

### 2. DealAuthorizationService (@Component "dealAuth")

| Method | Logic |
|--------|-------|
| `isParticipant(dealId)` | userId == deal.advertiserId OR userId == deal.ownerId |
| `isAdvertiser(dealId)` | userId == deal.advertiserId |
| `isOwner(dealId)` | userId == deal.ownerId |
| `channelId(dealId)` | Returns deal.channelId (for chaining with channelAuth) |

### 3. ChannelAuthorizationService (@Component "channelAuth")

| Method | Logic |
|--------|-------|
| `isOwner(channelId)` | membership.role == "OWNER" |
| `hasRight(channelId, right)` | role == "OWNER" OR rights.get(right) == true |

### 4. AuthorizationService (@Component "auth")

| Method | Logic |
|--------|-------|
| `isOperator()` | userContext.isOperator |

---

## @PreAuthorize Mapping

### Deal Endpoints

```
POST   /api/v1/deals                        isAuthenticated()
GET    /api/v1/deals/{id}                   @dealAuth.isParticipant(#id)
POST   /api/v1/deals/{id}/accept            @channelAuth.hasRight(@dealAuth.channelId(#id), 'moderate')
POST   /api/v1/deals/{id}/reject            @channelAuth.hasRight(@dealAuth.channelId(#id), 'moderate')
POST   /api/v1/deals/{id}/creative          @channelAuth.hasRight(@dealAuth.channelId(#id), 'moderate')
POST   /api/v1/deals/{id}/approve           @dealAuth.isAdvertiser(#id)
POST   /api/v1/deals/{id}/revision          @dealAuth.isAdvertiser(#id)
POST   /api/v1/deals/{id}/publish           @channelAuth.hasRight(@dealAuth.channelId(#id), 'publish')
POST   /api/v1/deals/{id}/dispute           @dealAuth.isParticipant(#id)
POST   /api/v1/deals/{id}/dispute/resolve   @auth.isOperator()
POST   /api/v1/deals/{id}/cancel            @dealAuth.isParticipant(#id)
```

### Channel Endpoints

```
GET    /api/v1/channels                     permitAll (public marketplace)
GET    /api/v1/channels/{id}                permitAll
PUT    /api/v1/channels/{id}                @channelAuth.isOwner(#id)
POST   /api/v1/channels/{id}/team           @channelAuth.hasRight(#id, 'manage_team')
DELETE /api/v1/channels/{id}/team/{userId}  @channelAuth.hasRight(#id, 'manage_team')
```

### Admin Endpoints

```
GET    /actuator/**                         @auth.isOperator()
GET    /api/v1/admin/reconciliation         @auth.isOperator()
POST   /api/v1/admin/reconciliation/trigger @auth.isOperator()
```

---

## Channel Membership Rights (JSONB)

```json
{
  "moderate": true,      // Accept/reject offers, manage creative
  "publish": true,       // Publish posts to channel
  "manage_team": false,  // Add/remove team members
  "view_stats": true     // View channel statistics
}
```

OWNER role implicitly has all rights (checked in `hasRight` method).

---

## Spring Security Filter Chain

```
Request -> JwtAuthenticationFilter -> SecurityFilterChain -> @PreAuthorize -> Controller
```

Order:
1. JWT filter extracts token, validates, populates `SecurityContext`
2. Spring Security checks `authorizeHttpRequests` rules
3. `@PreAuthorize` SpEL expressions evaluated before controller method
4. Authorization services load attributes on-demand (lazy, cached)

---

## Error Handling

| Scenario | Response |
|----------|----------|
| No token | 401 AUTH_INVALID_TOKEN |
| Invalid token | 401 AUTH_INVALID_TOKEN |
| Expired token | 401 AUTH_TOKEN_EXPIRED |
| Blacklisted token | 401 AUTH_TOKEN_REVOKED |
| @PreAuthorize fails | 403 AUTH_INSUFFICIENT_RIGHTS |
| Deal not found in auth check | 404 DEAL_NOT_FOUND |

---

## Testing Strategy

### Integration Tests

- Use `@WithMockUser` or custom `@WithTelegramUser` annotation
- Test each @PreAuthorize rule with allowed and denied cases
- Test membership caching (single DB call per channelId per request)

### Test Scenarios per Endpoint

1. Authenticated user with correct role -> 200
2. Authenticated user without permission -> 403
3. Unauthenticated -> 401
4. Non-participant accessing deal -> 403
5. Manager without required right -> 403

---

## Related Documents

- [Auth Flow](./03-auth-flow.md)
- [ABAC Pattern](../05-patterns-and-decisions/08-abac.md)
- [Team Management](../03-feature-specs/07-team-management.md)