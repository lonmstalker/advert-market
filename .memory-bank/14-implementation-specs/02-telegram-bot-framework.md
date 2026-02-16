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
    implementation 'com.github.pengrad:java-telegram-bot-api:9.3.0'
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
2. Enforce payload guardrail (`app.telegram.webhook.max-body-bytes`, default 262144) and return `413` for oversized requests
3. Parse request body via `BotUtils.parseUpdate(body)`
4. Delegate to `BotUpdateHandler.handle(update)`
5. Return 200 OK immediately (async processing)

Webhook rejection counters are tracked by reason:
- `invalid_secret`
- `parse_error`
- `oversize`
- `duplicate`

---

## Message Sending

### Notification Sender Pattern

1. Acquire per-chat rate limiter
2. Build `SendMessage(chatId, text)` with `ParseMode.MarkdownV2` and `disableWebPagePreview(true)`. Escape user data via `MarkdownV2Util.escape()`
3. Attach inline keyboard if buttons provided
4. Execute via `TelegramBot.execute(request)`
5. Log on failure: error code + description
6. Return success/failure boolean

---

## Message Templates (All 15 Types)

All messages use **MarkdownV2 parse mode**. User-provided data is escaped via `MarkdownV2Util.escape()`. Variables: `{channel_name}`, `{amount}`, `{deal_id_short}` (first 8 chars of UUID).

### .properties Escaping Gotcha (MarkdownV2)

If templates are stored in Java/Spring `.properties` bundles, remember that the properties parser treats backslash as an escape character.

To keep a literal backslash for Telegram MarkdownV2 escaping, you must **double-escape** it in the `.properties` file.

Example:
- File content: `Opening\\.` (two backslashes)
- Runtime value: `Opening\.` (single backslash)
- Telegram renders: `Opening.` (dot is escaped and accepted by MarkdownV2)

| # | Type | Recipient | Template (RU, MarkdownV2) |
|---|------|-----------|---------------|
| 1 | NEW_OFFER | Owner | *New offer* Channel: {channel\_name} Amount: {amount} TON |
| 2 | OFFER_ACCEPTED | Advertiser | *Offer accepted* Deposit {amount} TON |
| 3 | OFFER_REJECTED | Advertiser | Offer for {channel\_name} rejected |
| 4 | ESCROW_FUNDED | Owner | *Escrow is replenished* \#{deal\_id\_short} Prepare your creative |
| 5 | CREATIVE_SUBMITTED | Advertiser | Draft creative is ready for review |
| 6 | CREATIVE_APPROVED | Owner | Creative approved! Publish |
| 7 | REVISION_REQUESTED | Owner | Creative revision requested |
| 8 | PUBLISHED | Advertiser | Advertising published in {channel\_name}\! Verification 24h |
| 9 | DELIVERY_VERIFIED | Both | *Delivery confirmed* Payment processed |
| 10 | PAYOUT_SENT | Owner | *Payout {amount} TON* TX: {tx\_hash\_short} |
| 11 | DISPUTE_OPENED | Both | *Dispute open* \#{deal\_id\_short} |
| 12 | DISPUTE_RESOLVED | Both | Dispute resolved: {outcome} |
| 13 | DEAL_EXPIRED | Both | Deal \#{deal\_id\_short} expired |
| 14 | DEAL_CANCELLED | Other | Deal \#{deal\_id\_short} canceled |
| 15 | RECONCILIATION_ALERT | Operator | *ALERT: Reconciliation discrepancy* Type: {check\_type} |

Each message may include an inline keyboard button: `InlineKeyboardButton("Open deal").url(dealUrl)`.

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

### Command Registry

| Command | Description | Scope |
|---------|-------------|-------|
| `/start` | Welcome + WebApp launch | All users |
| `/start {startapp}` | Deep link routing | All users |
| `/language` | Language selection | All users |
| `/help` | Usage instructions | All users |
| `/mydeals` | Quick link to active deals | All users |
| `/balance` | Quick link to wallet | All users |
| `/status` | System status (uptime, API health) | Operator only |
| `/reconcile` | Trigger manual reconciliation | Operator only |

Commands are registered via `setMyCommands` on bot startup (public commands only).

### /start
- Upsert user in `users` table (create if not exists)
- Send welcome message with WebApp button linking to `https://app.advertmarket.com`
- If first-time user: trigger onboarding flow in Mini App

### /start {startapp}
- Parse deep link parameter (see Deep Link Routing below)
- Send message with inline WebApp button routing to specific screen

### /language
- Inline keyboard: `[RU Russian] [EN English]`
- Callback: save preference to user record, confirm with localized message

### /help
- Static message with commands list and FAQ links

### /mydeals, /balance
- Inline WebApp button linking directly to deals list / wallet page

### /status (operator-only)
- Verify `users.is_operator = true`
- Return: API uptime, active deals count, pending outbox count, Kafka consumer lag summary

### /reconcile (operator-only)
- Verify `users.is_operator = true`
- Publish trigger to `reconciliation.triggers` topic
- Reply: "Reconciliation triggered"

### Callback Query Handling

Inline keyboard callbacks follow pattern: `{action}:{entity_id}`:

| Callback Pattern | Action |
|-----------------|--------|
| `lang:ru` / `lang:en` | Set language preference |
| `open_deal:{uuid_short}` | Open deal in WebApp |
| `approve_creative:{uuid_short}` | Quick approve creative (transitions deal) |
| `reject_creative:{uuid_short}` | Quick reject creative (transitions deal) |

**Security**: All callback actions verify ABAC permissions before executing. State-changing callbacks re-check deal status to prevent stale actions.

---

## Deep Link Routing (startapp Parameter)

### Overview

Telegram Mini App deep links: `https://t.me/AdvertMarketBot/app?startapp=<payload>`. The `startapp` parameter is passed to the Mini App via `Telegram.WebApp.initDataUnsafe.start_param`.

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

`uuid_short` = first 8 characters of UUID (as in notification templates).

### Bot-Side Routing

When receiving `/start {startapp}` via webhook:

```java
public void handleStartCommand(Update update) {
    String startParam = extractStartParam(update.message().text()); // after "/start "
    if (startParam == null || startParam.isEmpty()) {
        sendWelcomeWithWebApp(update);
        return;
    }

    // Parse deep link
    String webAppUrl = buildDeepLinkUrl(startParam);
    var button = new InlineKeyboardButton("Open")
            .webApp(new WebAppInfo(webAppUrl));
    bot.execute(new SendMessage(chatId, "Opening...")
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

All notification templates with an "Open trade" button use deep link:

```java
String dealUrl = String.format("https://t.me/%s/app?startapp=deal_%s",
    botUsername, dealId.toString().substring(0, 8));
var button = new InlineKeyboardButton("Open deal").url(dealUrl);
```

### Security

- `startapp` parameter does NOT contain sensitive data (only type + short ID)
- Authorization is checked at the API level when requesting data (ABAC)
- Short UUID is not enough for brute-force (searching 16^8 = 4 billion options), but access is controlled via auth

---

## Related Documents

- [Notifications Feature](../03-feature-specs/08-notifications.md)
- [Outbox Poller](./11-outbox-poller.md)
- [Notification Templates](./22-notification-templates-i18n.md)
- [Auth Flow](./03-auth-flow.md)
