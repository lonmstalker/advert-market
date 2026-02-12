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

```properties
error.auth_invalid_init_data.title=Неверные данные Telegram
error.auth_invalid_init_data.detail=Подпись initData не прошла проверку
error.auth_expired_init_data.title=Данные initData устарели
error.auth_expired_init_data.detail=Время действия initData истекло, повторите авторизацию
error.auth_invalid_token.title=Неверный токен
error.auth_invalid_token.detail=JWT токен невалиден или повреждён
error.auth_token_expired.title=Токен истёк
error.auth_token_expired.detail=Срок действия JWT токена истёк
error.auth_insufficient_rights.title=Недостаточно прав
error.auth_insufficient_rights.detail=У вас нет прав для выполнения этого действия

error.deal_not_found.title=Сделка не найдена
error.deal_not_found.detail=Сделка с id {0} не найдена
error.deal_invalid_transition.title=Недопустимый переход
error.deal_invalid_transition.detail=Нельзя перевести сделку из состояния {0} в {1}

error.fin_insufficient_balance.title=Недостаточно средств
error.fin_insufficient_balance.detail=На счёте эскроу недостаточно средств
error.fin_amount_mismatch.title=Несовпадение суммы
error.fin_amount_mismatch.detail=Ожидалось {0} nanoTON, получено {1} nanoTON

error.val_invalid_input.title=Ошибка валидации
error.val_invalid_input.detail=Входные данные не прошли проверку
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
  "title": "Сделка не найдена",
  "status": 404,
  "detail": "Сделка с id 550e8400-e29b-41d4-a716-446655440000 не найдена",
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