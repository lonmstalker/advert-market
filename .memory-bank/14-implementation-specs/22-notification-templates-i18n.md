# Notification Templates & i18n

## Template Engine: Spring MessageSource

**Decision**: Spring `MessageSource` -- стандартный механизм i18n Spring Framework. Один `MessageSource` bean для всего приложения: уведомления, ошибки API, UI-тексты.

### Configuration

```yaml
spring:
  messages:
    basename: messages/notifications,messages/errors
    encoding: UTF-8
    fallback-to-system-locale: false
    default-locale: ru
```

Spring автоматически подхватывает файлы `messages/notifications_ru.properties`, `messages/notifications_en.properties` и т.д.

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

```properties
notification.NEW_OFFER=<b>Новое предложение</b>\nКанал: {0}\nСумма: {1} TON
notification.OFFER_ACCEPTED=<b>Предложение принято</b>\nВнесите депозит {0} TON
notification.OFFER_REJECTED=Предложение для {0} отклонено
notification.ESCROW_FUNDED=<b>Эскроу пополнен</b> #{0}\nПодготовьте креатив
notification.CREATIVE_SUBMITTED=Черновик креатива готов к проверке
notification.CREATIVE_APPROVED=Креатив одобрен! Публикуйте
notification.REVISION_REQUESTED=Запрошена доработка креатива
notification.PUBLISHED=Реклама опубликована в {0}!\nВерификация 24ч
notification.DELIVERY_VERIFIED=<b>Доставка подтверждена</b>\nВыплата обрабатывается
notification.PAYOUT_SENT=<b>Выплата {0} TON</b>\nTX: {1}
notification.DISPUTE_OPENED=<b>Открыт спор</b> #{0}
notification.DISPUTE_RESOLVED=Спор разрешён: {0}
notification.DEAL_EXPIRED=Сделка #{0} истекла
notification.DEAL_CANCELLED=Сделка #{0} отменена
notification.RECONCILIATION_ALERT=<b>ALERT: Расхождение при сверке</b>\nТип: {0}
```

### Template Keys Summary

| # | Key | RU |
|---|-----|------|
| 1 | `notification.NEW_OFFER` | Новое предложение, канал + сумма |
| 2 | `notification.OFFER_ACCEPTED` | Предложение принято, внесите депозит |
| 3 | `notification.OFFER_REJECTED` | Предложение отклонено |
| 4 | `notification.ESCROW_FUNDED` | Эскроу пополнен |
| 5 | `notification.CREATIVE_SUBMITTED` | Черновик готов к проверке |
| 6 | `notification.CREATIVE_APPROVED` | Креатив одобрен |
| 7 | `notification.REVISION_REQUESTED` | Запрос доработки |
| 8 | `notification.PUBLISHED` | Реклама опубликована |
| 9 | `notification.DELIVERY_VERIFIED` | Доставка подтверждена |
| 10 | `notification.PAYOUT_SENT` | Выплата отправлена |
| 11 | `notification.DISPUTE_OPENED` | Спор открыт |
| 12 | `notification.DISPUTE_RESOLVED` | Спор разрешён |
| 13 | `notification.DEAL_EXPIRED` | Сделка истекла |
| 14 | `notification.DEAL_CANCELLED` | Сделка отменена |
| 15 | `notification.RECONCILIATION_ALERT` | Алерт сверки |

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
    {"text": "Открыть сделку", "url": "https://t.me/bot?startapp=deal_550e8400"}
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

Один `MessageSource` для всего приложения:
- `notification.*` -- шаблоны уведомлений
- `error.*` -- локализованные ошибки API (см. [Error Code Catalog](./12-error-code-catalog.md))
- Можно расширять: `ui.*`, `bot.*` и т.д.

---

## Related Documents

- [Telegram Bot Framework](./02-telegram-bot-framework.md)
- [Notifications Feature](../03-feature-specs/08-notifications.md)
- [Outbox Poller](./11-outbox-poller.md)