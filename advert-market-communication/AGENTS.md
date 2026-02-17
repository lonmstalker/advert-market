# Parent Instructions
- Parent: `/Users/nikitakocnev/.codex/AGENTS.md`
- This file contains only module-local deltas.

# Communication — Agent Instructions

Telegram bot, webhook processing, channel verification, and notification delivery.

## Structure

| Area | Key Classes |
|------|------------|
| Bot dispatch | `BotDispatcher`, `HandlerRegistry`, `UpdateContext`, `BotCommand` interface |
| Commands | `StartCommand`, `LanguageCommand`, `LanguageCallbackHandler` |
| Channel events | `BotChannelStatusHandler` |
| Sender | `TelegramSender`, `TelegramRateLimiter`, `RateLimiterPort` |
| Webhook | `TelegramWebhookController`, `UpdateProcessor`, `UpdateDeduplicator` |
| Channel service | `TelegramChannelService`, `ChannelCachePort`, `RedisChannelCache` |
| Notifications | `TelegramNotificationService` |
| Canary | `CanaryRouter` — A/B routing for feature flags |
| Resilience | `TelegramResilienceProperties`, `TelegramCircuitBreakerConfig` |
| User state | `UserStatePort`, `RedisUserStateService` (FSM) |
| User block | `UserBlockPort`, `RedisUserBlockService` |

## Key Integrations

- **Telegram Bot API**: pengrad/java-telegram-bot-api 9.3.0
- **Resilience4j**: circuit breaker + rate limiter for Telegram API calls
- **Redis**: channel cache, rate limiting, user state (FSM), user block list, update deduplication

## Rules

- **MarkdownV2** parse mode for ALL Telegram messages — escape via `MarkdownV2Util`
- Mock async callbacks in tests: `doAnswer` for `TelegramBot.execute()` Callback parameter
- Rate limiting: `RateLimiterPort` backed by Redis + Resilience4j
- Bot handlers implement `MessageHandler`, `CallbackHandler`, or `ChatMemberUpdateHandler`
- `@ConfigurationProperties` for all bot/sender/cache/resilience settings
- Update deduplication via Redis to prevent duplicate webhook processing
