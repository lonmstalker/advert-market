# Error Code Catalog

## Response Format: RFC 9457 (previously RFC 7807)

All error responses follow RFC 9457 Problem Details:

```json
{
  "type": "urn:advertmarket:error:DEAL_NOT_FOUND",
  "title": "Deal not found",
  "status": 404,
  "detail": "Deal with id 550e8400-e29b-41d4-a716-446655440000 not found",
  "instance": "/api/v1/deals/550e8400-e29b-41d4-a716-446655440000",
  "error_code": "DEAL_NOT_FOUND",
  "timestamp": "2025-01-15T10:30:00Z",
  "correlation_id": "uuid..."
}
```

---

## Auth Errors

| Code | HTTP | Title |
|------|------|-------|
| `AUTH_INVALID_TOKEN` | 401 | Invalid or malformed JWT |
| `AUTH_TOKEN_EXPIRED` | 401 | JWT expired |
| `AUTH_INIT_DATA_INVALID` | 401 | Invalid Telegram initData (HMAC or anti-replay) |
| `AUTH_TOKEN_BLACKLISTED` | 401 | JWT revoked via blacklist |
| `AUTH_INSUFFICIENT_PERMISSIONS` | 403 | No permission for this action |
| `USER_BLOCKED` | 403 | User account is blocked |
| `ACCOUNT_DELETED` | 403 | User account soft-deleted |

---

## Deal Errors

| Code | HTTP | Title |
|------|------|-------|
| `DEAL_NOT_FOUND` | 404 | Deal not found |
| `DEAL_ALREADY_EXISTS` | 409 | Duplicate deal for channel |
| `DEAL_TERMS_MISMATCH` | 422 | Deal terms do not match |
| `DEAL_DEADLINE_EXPIRED` | 410 | Deal deadline has expired |
| `DEAL_CANCELLED` | 410 | Deal has been cancelled |

---

## Financial Errors

| Code | HTTP | Title |
|------|------|-------|
| `INSUFFICIENT_BALANCE` | 422 | Insufficient escrow balance |
| `DEPOSIT_TIMEOUT` | 408 | Deposit confirmation timeout |
| `DEPOSIT_AMOUNT_MISMATCH` | 422 | Deposit amount mismatch |
| `DEPOSIT_REJECTED` | 422 | Deposit rejected |
| `PAYOUT_FAILED` | 502 | Payout execution failed |
| `REFUND_FAILED` | 502 | Refund execution failed |
| `LEDGER_INCONSISTENCY` | 500 | Ledger integrity violation |
| `COMMISSION_CALCULATION_ERROR` | 500 | Commission calculation failed (overflow or invalid rate) |

---

## Channel Errors

| Code | HTTP | Title |
|------|------|-------|
| `CHANNEL_NOT_FOUND` | 404 | Channel not found |
| `CHANNEL_ALREADY_REGISTERED` | 409 | Channel already registered |
| `CHANNEL_INACCESSIBLE` | 502 | Cannot access channel via Telegram API |
| `CHANNEL_NOT_OWNED` | 403 | User is not the channel owner |
| `CHANNEL_STATS_UNAVAILABLE` | 503 | Channel statistics unavailable |
| `CHANNEL_BOT_NOT_MEMBER` | 403 | Bot is not a member of the channel |
| `CHANNEL_BOT_NOT_ADMIN` | 403 | Bot is not an admin of the channel |
| `CHANNEL_BOT_INSUFFICIENT_RIGHTS` | 403 | Bot lacks required admin rights |
| `CHANNEL_USER_NOT_ADMIN` | 403 | User is not a channel admin |

---

## Dispute Errors

| Code | HTTP | Title |
|------|------|-------|
| `DISPUTE_NOT_FOUND` | 404 | Dispute not found |
| `DISPUTE_ALREADY_EXISTS` | 409 | Dispute already open for this deal |
| `DISPUTE_RESOLUTION_FAILED` | 500 | Dispute resolution failed |

---

## Creative Errors

| Code | HTTP | Title |
|------|------|-------|
| `CREATIVE_NOT_FOUND` | 404 | Creative not found |
| `CREATIVE_INVALID_FORMAT` | 422 | Invalid creative format |
| `CREATIVE_TOO_LARGE` | 413 | Creative exceeds size limit |
| `CREATIVE_REJECTED` | 422 | Creative rejected |

---

## Delivery Errors

| Code | HTTP | Title |
|------|------|-------|
| `DELIVERY_VERIFICATION_FAILED` | 502 | Delivery verification failed |
| `DELIVERY_POST_DELETED` | 410 | Post was deleted |
| `DELIVERY_CONTENT_MODIFIED` | 409 | Post content was modified |
| `DELIVERY_TIMEOUT` | 408 | Delivery verification timeout |

---

## Validation Errors

| Code | HTTP | Title |
|------|------|-------|
| `VALIDATION_FAILED` | 400 | Input validation failed |
| `INVALID_PARAMETER` | 400 | Invalid parameter value |
| `MISSING_REQUIRED_FIELD` | 400 | Required field missing |

For validation errors, include `violations` array:
```json
{
  "error_code": "VAL_INVALID_INPUT",
  "status": 400,
  "violations": [
    {"field": "amount_nano", "message": "must be greater than 0"},
    {"field": "channel_id", "message": "must not be null"}
  ]
}
```

---

## Entity Errors

| Code | HTTP | Title |
|------|------|-------|
| `ENTITY_NOT_FOUND` | 404 | Generic entity not found |
| `USER_NOT_FOUND` | 404 | User not found |
| `WALLET_NOT_FOUND` | 404 | Wallet not found |
| `INVALID_STATE_TRANSITION` | 409 | Invalid state machine transition |
| `NOTIFICATION_DELIVERY_FAILED` | 502 | Notification delivery failed |

---

## System Errors

| Code | HTTP | Title |
|------|------|-------|
| `INTERNAL_ERROR` | 500 | Internal server error |
| `SERVICE_UNAVAILABLE` | 503 | Service temporarily unavailable |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |

---

## Infrastructure Errors

| Code | HTTP | Title |
|------|------|-------|
| `EVENT_DESERIALIZATION_ERROR` | 500 | Event deserialization failed |
| `JSON_ERROR` | 500 | JSON processing error |
| `LOCK_ACQUISITION_FAILED` | 409 | Distributed lock acquisition failed |
| `OUTBOX_PUBLISH_FAILED` | 500 | Outbox publish failed |

---

## Java Implementation

Two complementary classes in `advert-market-shared`:

- **`ErrorCode`** (enum) — maps code → HTTP status, provides `titleKey()`, `detailKey()`, `typeUri()`, `resolve(String)`. URI format: `urn:advertmarket:error:{CODE}`.
- **`ErrorCodes`** (constants class) — `@Fenum(FenumGroup.ERROR_CODE)` annotated `String` constants for compile-time type safety via Checker Framework. Used in `DomainException` constructors.

---

## Localization via Spring MessageSource

Error titles and detail messages are localized using Spring `MessageSource`, same as notification templates.

### Resource Files

```
src/main/resources/
  errors_ru.properties
  errors_en.properties
```

### Example: errors_ru.properties

RU sample content is intentionally stored with `\uXXXX` escapes to satisfy the English-only documentation policy while preserving locale semantics.

```properties
error.auth_invalid_init_data.title=\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0435 \u0434\u0430\u043d\u043d\u044b\u0435 Telegram
error.auth_invalid_init_data.detail=\u041f\u043e\u0434\u043f\u0438\u0441\u044c initData \u043d\u0435 \u043f\u0440\u043e\u0448\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0443
error.auth_expired_init_data.title=\u0414\u0430\u043d\u043d\u044b\u0435 initData \u0443\u0441\u0442\u0430\u0440\u0435\u043b\u0438
error.auth_expired_init_data.detail=\u0412\u0440\u0435\u043c\u044f \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f initData \u0438\u0441\u0442\u0435\u043a\u043b\u043e, \u043f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u0435 \u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u044e
error.auth_invalid_token.title=\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u0442\u043e\u043a\u0435\u043d
error.auth_invalid_token.detail=JWT \u0442\u043e\u043a\u0435\u043d \u043d\u0435\u0432\u0430\u043b\u0438\u0434\u0435\u043d \u0438\u043b\u0438 \u043f\u043e\u0432\u0440\u0435\u0436\u0434\u0451\u043d
error.auth_token_expired.title=\u0422\u043e\u043a\u0435\u043d \u0438\u0441\u0442\u0451\u043a
error.auth_token_expired.detail=\u0421\u0440\u043e\u043a \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f JWT \u0442\u043e\u043a\u0435\u043d\u0430 \u0438\u0441\u0442\u0451\u043a
error.auth_insufficient_rights.title=\u041d\u0435\u0434\u043e\u0441\u0442\u0430\u0442\u043e\u0447\u043d\u043e \u043f\u0440\u0430\u0432
error.auth_insufficient_rights.detail=\u0423 \u0432\u0430\u0441 \u043d\u0435\u0442 \u043f\u0440\u0430\u0432 \u0434\u043b\u044f \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f \u044d\u0442\u043e\u0433\u043e \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f

error.deal_not_found.title=\u0421\u0434\u0435\u043b\u043a\u0430 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430
error.deal_not_found.detail=\u0421\u0434\u0435\u043b\u043a\u0430 \u0441 id {0} \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430
error.deal_invalid_transition.title=\u041d\u0435\u0434\u043e\u043f\u0443\u0441\u0442\u0438\u043c\u044b\u0439 \u043f\u0435\u0440\u0435\u0445\u043e\u0434
error.deal_invalid_transition.detail=\u041d\u0435\u043b\u044c\u0437\u044f \u043f\u0435\u0440\u0435\u0432\u0435\u0441\u0442\u0438 \u0441\u0434\u0435\u043b\u043a\u0443 \u0438\u0437 \u0441\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u044f {0} \u0432 {1}

error.fin_insufficient_balance.title=\u041d\u0435\u0434\u043e\u0441\u0442\u0430\u0442\u043e\u0447\u043d\u043e \u0441\u0440\u0435\u0434\u0441\u0442\u0432
error.fin_insufficient_balance.detail=\u041d\u0430 \u0441\u0447\u0451\u0442\u0435 \u044d\u0441\u043a\u0440\u043e\u0443 \u043d\u0435\u0434\u043e\u0441\u0442\u0430\u0442\u043e\u0447\u043d\u043e \u0441\u0440\u0435\u0434\u0441\u0442\u0432
error.fin_amount_mismatch.title=\u041d\u0435\u0441\u043e\u0432\u043f\u0430\u0434\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b
error.fin_amount_mismatch.detail=\u041e\u0436\u0438\u0434\u0430\u043b\u043e\u0441\u044c {0} nanoTON, \u043f\u043e\u043b\u0443\u0447\u0435\u043d\u043e {1} nanoTON

error.val_invalid_input.title=\u041e\u0448\u0438\u0431\u043a\u0430 \u0432\u0430\u043b\u0438\u0434\u0430\u0446\u0438\u0438
error.val_invalid_input.detail=\u0412\u0445\u043e\u0434\u043d\u044b\u0435 \u0434\u0430\u043d\u043d\u044b\u0435 \u043d\u0435 \u043f\u0440\u043e\u0448\u043b\u0438 \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0443
```

### Example: errors_en.properties

```properties
error.auth_invalid_init_data.title=Invalid Telegram initData
error.auth_invalid_init_data.detail=initData HMAC signature verification failed
error.deal_not_found.title=Deal not found
error.deal_not_found.detail=Deal with id {0} not found
# ... all codes
```

### Localized Response Example

```json
{
  "type": "https://api.advertmarket.com/errors/DEAL_NOT_FOUND",
  "title": "\u0421\u0434\u0435\u043b\u043a\u0430 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430",
  "status": 404,
  "detail": "\u0421\u0434\u0435\u043b\u043a\u0430 \u0441 id 550e8400-e29b-41d4-a716-446655440000 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430",
  "error_code": "DEAL_NOT_FOUND",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### Locale Resolution for Errors

1. `Accept-Language` header from request
2. User's `language_code` from JWT context
3. Fallback: `ru`

---

## Related Documents

- [API Contracts](../11-api-contracts.md)
- [Auth Flow](./03-auth-flow.md)
- [Deal State Machine](../06-deal-state-machine.md)
- [Notification Templates](./22-notification-templates-i18n.md)
