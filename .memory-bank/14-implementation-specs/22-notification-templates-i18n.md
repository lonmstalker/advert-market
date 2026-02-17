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
notification.NEW_OFFER=<b>New offer</b>\nChannel: {0}\nAmount: {1} TON
notification.OFFER_ACCEPTED=<b>Offer accepted</b>\nMake a deposit {0} TON
notification.OFFER_REJECTED=Offer for {0} has been rejected
notification.ESCROW_FUNDED=<b>Escrow is replenished</b> #{0}\nPrepare creative
notification.CREATIVE_SUBMITTED=Draft creative is ready for review
notification.CREATIVE_APPROVED=Creative has been approved! Publish
notification.REVISION_REQUESTED=Creative revision requested
notification.PUBLISHED=Advertisement published in {0}!\n24h verification
notification.DELIVERY_VERIFIED=<b>Delivery confirmed</b>\nPayment is being processed
notification.PAYOUT_SENT=<b>Payout {0} TON</b>\nTX: {1}
notification.DISPUTE_OPENED=<b>Dispute open</b> #{0}
notification.DISPUTE_RESOLVED=Dispute resolved: {0}
notification.DEAL_EXPIRED=Trade #{0} has expired
notification.DEAL_CANCELLED=Deal #{0} canceled
notification.RECONCILIATION_ALERT=<b>ALERT: Reconciliation discrepancy</b>\nType: {0}
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
    {"text": "Open deal", "url": "https://t.me/adv_markt_bot/app?startapp=deal_550e8400"}
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
