# Telegram Bot Framework & Webhook Setup

## Framework Choice: pengrad/java-telegram-bot-api

**Decision**: `com.github.pengrad:java-telegram-bot-api` -- lightweight Java library.

| Criterion | pengrad | TelegramBots (rubenlagus) | Raw WebClient |
|-----------|---------|---------------------------|---------------|
| Size | ~500KB, minimal deps | ~5MB, many transitive deps | 0 extra |
| API style | Clean builder pattern | Verbose, inheritance-based | Manual JSON |
| Java | Excellent (builders + fluent API) | OK | OK |
| Webhook | Built-in support | Built-in | Manual |
| Maintenance | Active, fast releases | Active | N/A |

### Gradle

```groovy
dependencies {
    implementation 'com.github.pengrad:java-telegram-bot-api:7.11.0'
}
```

---

## Webhook Setup

### Registration

```
POST https://api.telegram.org/bot{token}/setWebhook
{
  "url": "https://bot.advertmarket.com/api/v1/bot/webhook",
  "secret_token": "<random-256-bit-hex>",
  "allowed_updates": ["message", "callback_query"],
  "max_connections": 40
}
```

Programmatic registration on startup via `TelegramBot.execute(SetWebhook(...))`.

### Requirements

| Aspect | Value |
|--------|-------|
| Protocol | HTTPS (TLS 1.2+) |
| Port | 443 (recommended) |
| Certificate | Valid public CA (Let's Encrypt) |
| Path | `/api/v1/bot/webhook` |
| Verification | `X-Telegram-Bot-Api-Secret-Token` header |
| Response | Must respond 200 within 60 seconds |

### Spring Boot Webhook Controller

Endpoint `POST /api/v1/bot/webhook`:
1. Validate `X-Telegram-Bot-Api-Secret-Token` header matches configured secret
2. Parse request body via `BotUtils.parseUpdate(body)`
3. Delegate to `BotUpdateHandler.handle(update)`
4. Return 200 OK immediately (async processing)

---

## Message Sending

### Notification Sender Pattern

1. Acquire per-chat rate limiter
2. Build `SendMessage(chatId, text)` with `ParseMode.HTML` and `disableWebPagePreview(true)`
3. Attach inline keyboard if buttons provided
4. Execute via `TelegramBot.execute(request)`
5. Log on failure: error code + description
6. Return success/failure boolean

---

## Message Templates (All 15 Types)

All messages use **HTML parse mode**. Variables: `{channel_name}`, `{amount}`, `{deal_id_short}` (first 8 chars of UUID).

| # | Type | Recipient | Template (RU) |
|---|------|-----------|---------------|
| 1 | NEW_OFFER | Owner | `<b>Новое предложение</b>` Канал: {channel_name} Сумма: {amount} TON |
| 2 | OFFER_ACCEPTED | Advertiser | `<b>Предложение принято</b>` Внесите депозит {amount} TON |
| 3 | OFFER_REJECTED | Advertiser | Предложение для {channel_name} отклонено |
| 4 | ESCROW_FUNDED | Owner | `<b>Эскроу пополнен</b>` #{deal_id_short} Подготовьте креатив |
| 5 | CREATIVE_SUBMITTED | Advertiser | Черновик креатива готов к проверке |
| 6 | CREATIVE_APPROVED | Owner | Креатив одобрен! Публикуйте |
| 7 | REVISION_REQUESTED | Owner | Запрошена доработка креатива |
| 8 | PUBLISHED | Advertiser | Реклама опубликована в {channel_name}! Верификация 24ч |
| 9 | DELIVERY_VERIFIED | Both | `<b>Доставка подтверждена</b>` Выплата обрабатывается |
| 10 | PAYOUT_SENT | Owner | `<b>Выплата {amount} TON</b>` TX: {tx_hash_short} |
| 11 | DISPUTE_OPENED | Both | `<b>Открыт спор</b>` #{deal_id_short} |
| 12 | DISPUTE_RESOLVED | Both | Спор разрешён: {outcome} |
| 13 | DEAL_EXPIRED | Both | Сделка #{deal_id_short} истекла |
| 14 | DEAL_CANCELLED | Other | Сделка #{deal_id_short} отменена |
| 15 | RECONCILIATION_ALERT | Operator | `<b>ALERT: Расхождение при сверке</b>` Тип: {check_type} |

Each message may include an inline keyboard button: `InlineKeyboardButton("Открыть сделку").url(dealUrl)`.

---

## Bot Token & Configuration

```yaml
telegram:
  bot:
    token: ${TELEGRAM_BOT_TOKEN}
    webhook:
      url: ${TELEGRAM_WEBHOOK_URL:https://bot.advertmarket.com/api/v1/bot/webhook}
      secret: ${TELEGRAM_WEBHOOK_SECRET}
  rate-limit:
    per-chat-delay-ms: 1000   # 1 msg/sec to same chat
    global-per-sec: 30         # 30 msg/sec to different chats
```

---

## Error Handling & Retry

### Telegram API Error Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | -- |
| 400 | Bad request | No retry, log |
| 401 | Invalid token | No retry, ALERT |
| 403 | Bot blocked by user | No retry, mark unreachable |
| 429 | Rate limit | Retry after `retry_after` seconds |
| 500+ | Server error | Retry with exponential backoff |

### Retry Strategy

- Max retries: 3
- Backoff: 1s -> 2s -> 4s (exponential)
- For 429: use `parameters.retry_after` from response
- After max retries: mark notification FAILED in outbox

### Rate Limiting

| Limit | Value |
|-------|-------|
| Different chats | 30 msg/sec |
| Same chat | 1 msg/sec |
| Same group | 20 msg/min |

Implementation: per-chatId `RateLimiter` + global semaphore.

---

## Bot Commands

### /start
- Upsert user in `users` table
- Send welcome with WebApp button linking to `https://app.advertmarket.com`

### /language
- Inline keyboard: `[RU Русский] [EN English]`
- Callback: save preference, confirm

---

## Deep Link Routing (startapp Parameter)

### Overview

Telegram Mini App deep links: `https://t.me/AdvertMarketBot/app?startapp=<payload>`. Параметр `startapp` передаётся в Mini App через `Telegram.WebApp.initDataUnsafe.start_param`.

### Link Format

```
https://t.me/{BotUsername}/app?startapp={action}_{id}
```

| Pattern | Example | Target Screen |
|---------|---------|--------------|
| `deal_{uuid_short}` | `startapp=deal_550e8400` | Deal details page |
| `channel_{id}` | `startapp=channel_123456789` | Channel listing page |
| `dispute_{uuid_short}` | `startapp=dispute_550e8400` | Dispute details page |
| `deposit_{uuid_short}` | `startapp=deposit_550e8400` | Deposit status page |

`uuid_short` = первые 8 символов UUID (как в notification templates).

### Bot-Side Routing

При получении `/start {startapp}` через webhook:

```java
public void handleStartCommand(Update update) {
    String startParam = extractStartParam(update.message().text()); // after "/start "
    if (startParam == null || startParam.isEmpty()) {
        sendWelcomeWithWebApp(update);
        return;
    }

    // Parse deep link
    String webAppUrl = buildDeepLinkUrl(startParam);
    var button = new InlineKeyboardButton("Открыть")
            .webApp(new WebAppInfo(webAppUrl));
    bot.execute(new SendMessage(chatId, "Открываю...")
            .replyMarkup(new InlineKeyboardMarkup(button)));
}

private String buildDeepLinkUrl(String startParam) {
    // startapp=deal_550e8400 -> https://app.advertmarket.com/?route=deal&id=550e8400
    String[] parts = startParam.split("_", 2);
    if (parts.length != 2) return BASE_WEBAPP_URL;

    return switch (parts[0]) {
        case "deal" -> BASE_WEBAPP_URL + "?route=deals/" + parts[1];
        case "channel" -> BASE_WEBAPP_URL + "?route=channels/" + parts[1];
        case "dispute" -> BASE_WEBAPP_URL + "?route=disputes/" + parts[1];
        case "deposit" -> BASE_WEBAPP_URL + "?route=deposits/" + parts[1];
        default -> BASE_WEBAPP_URL;
    };
}
```

### Mini App-Side Routing

```javascript
// On Mini App init
const startParam = window.Telegram.WebApp.initDataUnsafe.start_param;
if (startParam) {
    const [action, id] = startParam.split('_');
    router.push(`/${action}s/${id}`);
}
```

### Notification Template Integration

Все notification templates с кнопкой "Открыть сделку" используют deep link:

```java
String dealUrl = String.format("https://t.me/%s/app?startapp=deal_%s",
    botUsername, dealId.toString().substring(0, 8));
var button = new InlineKeyboardButton("Открыть сделку").url(dealUrl);
```

### Security

- `startapp` параметр НЕ содержит чувствительных данных (только тип + short ID)
- Авторизация проверяется на уровне API при запросе данных (ABAC)
- Short UUID недостаточен для brute-force (перебор 16^8 = 4 млрд вариантов), но доступ контролируется через auth

---

## Related Documents

- [Notifications Feature](../03-feature-specs/08-notifications.md)
- [Outbox Poller](./11-outbox-poller.md)
- [Notification Templates](./22-notification-templates-i18n.md)
- [Auth Flow](./03-auth-flow.md)