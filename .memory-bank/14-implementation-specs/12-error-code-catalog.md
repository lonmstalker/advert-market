# Error Code Catalog

## Response Format: RFC 7807

All error responses follow RFC 7807 Problem Details:

```json
{
  "type": "https://api.advertmarket.com/errors/DEAL_NOT_FOUND",
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

## Auth Errors (AUTH_*)

| Code | HTTP | Title |
|------|------|-------|
| `AUTH_INVALID_INIT_DATA` | 401 | Invalid Telegram initData |
| `AUTH_EXPIRED_INIT_DATA` | 401 | initData expired (anti-replay) |
| `AUTH_INVALID_TOKEN` | 401 | Invalid or malformed JWT |
| `AUTH_TOKEN_EXPIRED` | 401 | JWT expired |
| `AUTH_TOKEN_REVOKED` | 401 | JWT blacklisted |
| `AUTH_INSUFFICIENT_RIGHTS` | 403 | No permission for this action |
| `AUTH_NOT_PARTICIPANT` | 403 | Not a deal participant |
| `AUTH_NOT_OPERATOR` | 403 | Operator access required |

---

## Deal Errors (DEAL_*)

| Code | HTTP | Title |
|------|------|-------|
| `DEAL_NOT_FOUND` | 404 | Deal not found |
| `DEAL_INVALID_TRANSITION` | 409 | Invalid state transition |
| `DEAL_ALREADY_EXISTS` | 409 | Duplicate deal for channel |
| `DEAL_EXPIRED` | 410 | Deal has expired |
| `DEAL_LOCKED` | 409 | Deal is being processed |

---

## Financial Errors (FIN_*)

| Code | HTTP | Title |
|------|------|-------|
| `FIN_INSUFFICIENT_BALANCE` | 422 | Insufficient escrow balance |
| `FIN_ESCROW_ALREADY_FUNDED` | 409 | Escrow already funded |
| `FIN_PAYOUT_IN_PROGRESS` | 409 | Payout already in progress |
| `FIN_REFUND_IN_PROGRESS` | 409 | Refund already in progress |
| `FIN_AMOUNT_MISMATCH` | 422 | Deposit amount mismatch |
| `FIN_INVALID_AMOUNT` | 422 | Amount must be positive |
| `FIN_TX_NOT_FOUND` | 404 | Transaction not found |

---

## Channel Errors (CHAN_*)

| Code | HTTP | Title |
|------|------|-------|
| `CHAN_NOT_FOUND` | 404 | Channel not found |
| `CHAN_NOT_ACTIVE` | 422 | Channel is deactivated |
| `CHAN_ALREADY_REGISTERED` | 409 | Channel already registered |
| `CHAN_MEMBER_EXISTS` | 409 | User already a team member |
| `CHAN_MEMBER_NOT_FOUND` | 404 | Team member not found |
| `CHAN_CANNOT_REMOVE_OWNER` | 422 | Cannot remove channel owner |

---

## Dispute Errors (DISP_*)

| Code | HTTP | Title |
|------|------|-------|
| `DISP_ALREADY_OPEN` | 409 | Dispute already open for this deal |
| `DISP_NOT_FOUND` | 404 | Dispute not found |
| `DISP_ALREADY_RESOLVED` | 409 | Dispute already resolved |
| `DISP_INVALID_EVIDENCE` | 422 | Invalid evidence format |

---

## Creative Errors (CRTV_*)

| Code | HTTP | Title |
|------|------|-------|
| `CRTV_BRIEF_REQUIRED` | 422 | Creative brief is required |
| `CRTV_DRAFT_INVALID` | 422 | Creative draft validation failed |
| `CRTV_TEXT_TOO_LONG` | 422 | Text exceeds maximum length |
| `CRTV_MEDIA_LIMIT` | 422 | Too many media attachments |

---

## Validation Errors (VAL_*)

| Code | HTTP | Title |
|------|------|-------|
| `VAL_INVALID_INPUT` | 400 | Input validation failed |
| `VAL_MISSING_FIELD` | 400 | Required field missing |
| `VAL_INVALID_FORMAT` | 400 | Invalid field format |

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

## System Errors (SYS_*)

| Code | HTTP | Title |
|------|------|-------|
| `SYS_INTERNAL_ERROR` | 500 | Internal server error |
| `SYS_SERVICE_UNAVAILABLE` | 503 | Service temporarily unavailable |
| `SYS_TON_API_ERROR` | 502 | TON API communication error |
| `SYS_TELEGRAM_API_ERROR` | 502 | Telegram API communication error |
| `SYS_RATE_LIMITED` | 429 | Too many requests |

---

## Java Enum Structure

```java
public enum ErrorCode {
    AUTH_INVALID_INIT_DATA(401),
    AUTH_EXPIRED_INIT_DATA(401),
    AUTH_INVALID_TOKEN(401),
    AUTH_TOKEN_EXPIRED(401),
    AUTH_TOKEN_REVOKED(401),
    AUTH_INSUFFICIENT_RIGHTS(403),
    // ... all codes
    ;

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    // title and detail resolved via MessageSource
    public String messageKey() {
        return "error." + name().toLowerCase();
    }
}
```

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