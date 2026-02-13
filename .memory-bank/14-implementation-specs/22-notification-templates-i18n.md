# Notification Templates & i18n

## Template Engine: Spring MessageSource

**Decision**: Spring `MessageSource` is a standard i18n Spring Framework mechanism. One `MessageSource` bean for the entire application: notifications, API errors, UI texts.

### Configuration

```yaml
spring:
  messages:
    basename: messages/notifications,messages/errors
    encoding: UTF-8
    fallback-to-system-locale: false
    default-locale: ru
```

Spring automatically picks up files `messages/notifications_ru.properties`, `messages/notifications_en.properties`, etc.

---

## Template Registry

### Resource Bundle Files

```
src/main/resources/messages/
  notifications_ru.properties
  notifications_en.properties
  errors_ru.properties
  errors_en.properties
```

### notifications_ru.properties

RU sample content is intentionally stored with `\uXXXX` escapes to satisfy the English-only documentation policy while preserving locale semantics.

```properties
notification.NEW_OFFER=<b>\u041d\u043e\u0432\u043e\u0435 \u043f\u0440\u0435\u0434\u043b\u043e\u0436\u0435\u043d\u0438\u0435</b>\n\u041a\u0430\u043d\u0430\u043b: {0}\n\u0421\u0443\u043c\u043c\u0430: {1} TON
notification.OFFER_ACCEPTED=<b>\u041f\u0440\u0435\u0434\u043b\u043e\u0436\u0435\u043d\u0438\u0435 \u043f\u0440\u0438\u043d\u044f\u0442\u043e</b>\n\u0412\u043d\u0435\u0441\u0438\u0442\u0435 \u0434\u0435\u043f\u043e\u0437\u0438\u0442 {0} TON
notification.OFFER_REJECTED=\u041f\u0440\u0435\u0434\u043b\u043e\u0436\u0435\u043d\u0438\u0435 \u0434\u043b\u044f {0} \u043e\u0442\u043a\u043b\u043e\u043d\u0435\u043d\u043e
notification.ESCROW_FUNDED=<b>\u042d\u0441\u043a\u0440\u043e\u0443 \u043f\u043e\u043f\u043e\u043b\u043d\u0435\u043d</b> #{0}\n\u041f\u043e\u0434\u0433\u043e\u0442\u043e\u0432\u044c\u0442\u0435 \u043a\u0440\u0435\u0430\u0442\u0438\u0432
notification.CREATIVE_SUBMITTED=\u0427\u0435\u0440\u043d\u043e\u0432\u0438\u043a \u043a\u0440\u0435\u0430\u0442\u0438\u0432\u0430 \u0433\u043e\u0442\u043e\u0432 \u043a \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0435
notification.CREATIVE_APPROVED=\u041a\u0440\u0435\u0430\u0442\u0438\u0432 \u043e\u0434\u043e\u0431\u0440\u0435\u043d! \u041f\u0443\u0431\u043b\u0438\u043a\u0443\u0439\u0442\u0435
notification.REVISION_REQUESTED=\u0417\u0430\u043f\u0440\u043e\u0448\u0435\u043d\u0430 \u0434\u043e\u0440\u0430\u0431\u043e\u0442\u043a\u0430 \u043a\u0440\u0435\u0430\u0442\u0438\u0432\u0430
notification.PUBLISHED=\u0420\u0435\u043a\u043b\u0430\u043c\u0430 \u043e\u043f\u0443\u0431\u043b\u0438\u043a\u043e\u0432\u0430\u043d\u0430 \u0432 {0}!\n\u0412\u0435\u0440\u0438\u0444\u0438\u043a\u0430\u0446\u0438\u044f 24\u0447
notification.DELIVERY_VERIFIED=<b>\u0414\u043e\u0441\u0442\u0430\u0432\u043a\u0430 \u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0430</b>\n\u0412\u044b\u043f\u043b\u0430\u0442\u0430 \u043e\u0431\u0440\u0430\u0431\u0430\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f
notification.PAYOUT_SENT=<b>\u0412\u044b\u043f\u043b\u0430\u0442\u0430 {0} TON</b>\nTX: {1}
notification.DISPUTE_OPENED=<b>\u041e\u0442\u043a\u0440\u044b\u0442 \u0441\u043f\u043e\u0440</b> #{0}
notification.DISPUTE_RESOLVED=\u0421\u043f\u043e\u0440 \u0440\u0430\u0437\u0440\u0435\u0448\u0451\u043d: {0}
notification.DEAL_EXPIRED=\u0421\u0434\u0435\u043b\u043a\u0430 #{0} \u0438\u0441\u0442\u0435\u043a\u043b\u0430
notification.DEAL_CANCELLED=\u0421\u0434\u0435\u043b\u043a\u0430 #{0} \u043e\u0442\u043c\u0435\u043d\u0435\u043d\u0430
notification.RECONCILIATION_ALERT=<b>ALERT: \u0420\u0430\u0441\u0445\u043e\u0436\u0434\u0435\u043d\u0438\u0435 \u043f\u0440\u0438 \u0441\u0432\u0435\u0440\u043a\u0435</b>\n\u0422\u0438\u043f: {0}
```

### Template Keys Summary

| # | Key | RU |
|---|-----|------|
| 1 | `notification.NEW_OFFER` | New offer, channel + amount |
| 2 | `notification.OFFER_ACCEPTED` | Offer Accepted, Please Make a Deposit |
| 3 | `notification.OFFER_REJECTED` | Offer rejected |
| 4 | `notification.ESCROW_FUNDED` | Escrow replenished |
| 5 | `notification.CREATIVE_SUBMITTED` | Draft ready for review |
| 6 | `notification.CREATIVE_APPROVED` | Creative approved |
| 7 | `notification.REVISION_REQUESTED` | Revision request |
| 8 | `notification.PUBLISHED` | Advertisement published |
| 9 | `notification.DELIVERY_VERIFIED` | Delivery confirmed |
| 10 | `notification.PAYOUT_SENT` | Payment sent |
| 11 | `notification.DISPUTE_OPENED` | The dispute is open |
| 12 | `notification.DISPUTE_RESOLVED` | Dispute resolved |
| 13 | `notification.DEAL_EXPIRED` | Deal expired |
| 14 | `notification.DEAL_CANCELLED` | Deal canceled |
| 15 | `notification.RECONCILIATION_ALERT` | Reconciliation alert |

---

## notification_outbox.payload Schema

```json
{
  "recipient_id": 42,
  "template": "ESCROW_FUNDED",
  "locale": "ru",
  "vars": {
    "channel_name": "Tech Channel",
    "amount": "100",
    "deal_id_short": "550e8400",
    "tx_hash_short": "abc123",
    "outcome": "REFUND"
  },
  "buttons": [
    {"text": "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0441\u0434\u0435\u043b\u043a\u0443", "url": "https://t.me/bot?startapp=deal_550e8400"}
  ]
}
```

### Var Mapping to MessageFormat Placeholders

| Template | {0} | {1} | {2} |
|----------|-----|-----|-----|
| NEW_OFFER | channel_name | amount | -- |
| OFFER_ACCEPTED | amount | -- | -- |
| OFFER_REJECTED | channel_name | -- | -- |
| ESCROW_FUNDED | deal_id_short | -- | -- |
| PUBLISHED | channel_name | -- | -- |
| PAYOUT_SENT | amount | tx_hash_short | -- |
| DISPUTE_OPENED | deal_id_short | -- | -- |
| DISPUTE_RESOLVED | outcome | -- | -- |
| DEAL_EXPIRED | deal_id_short | -- | -- |
| DEAL_CANCELLED | deal_id_short | -- | -- |
| RECONCILIATION_ALERT | check_type | -- | -- |

---

## Multi-Language Support

### /language Command

User selects language via Telegram bot command:
1. Bot sends inline keyboard with language options
2. User taps choice
3. Store `language_code` in `users` table
4. All future notifications use selected locale

### Locale Resolution

1. Check `users.language_code`
2. Fallback: `ru` (default)
3. If template not found for locale: fallback to `ru`

### Adding New Language

1. Create `messages_{locale}.properties`
2. Translate all 15 templates
3. Add locale option to /language keyboard
4. No code changes needed

---

## Rendering Flow

1. Read outbox record
2. Resolve locale from user's `language_code`
3. Call `messageSource.getMessage("notification." + template, vars, locale)`
4. Build inline keyboard from `buttons` array
5. Send via `TelegramMessageSender`

### Shared MessageSource Bean

One `MessageSource` for the entire application:
- `notification.*` -- notification templates
- `error.*` -- localized API errors (see [Error Code Catalog](./12-error-code-catalog.md))
- Can be expanded: `ui.*`, `bot.*`, etc.

---

## Related Documents

- [Telegram Bot Framework](./02-telegram-bot-framework.md)
- [Notifications Feature](../03-feature-specs/08-notifications.md)
- [Outbox Poller](./11-outbox-poller.md)
