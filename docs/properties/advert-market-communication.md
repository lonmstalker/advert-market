# advert-market-communication

## Table of Contents

- [Telegram Bot](#telegram-bot)
- [User Blocking](#user-blocking)
- [Channel Cache](#channel-cache)
- [Update Deduplication](#update-deduplication)
- [Telegram Resilience](#telegram-resilience)
- [Telegram Retry](#telegram-retry)
- [Telegram Sender](#telegram-sender)
- [User State](#user-state)


---

## Telegram Bot

Root configuration for the Telegram bot


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.telegram.bot-token` | `NotBlank String` | Telegram bot token from BotFather |  | Yes |  | `123456:ABC-DEF...` |
| `app.telegram.bot-username` | `NotBlank String` | Telegram bot username |  | Yes |  |  |
| `app.telegram.webhook.url` | `String` | Webhook URL for receiving updates |  | No |  |  |
| `app.telegram.webhook.secret` | `NotBlank String` | Secret token for webhook validation |  | No |  |  |
| `app.telegram.webapp.url` | `NotBlank String` | Telegram Web App URL |  | Yes |  |  |
| `app.telegram.welcome.custom-emoji-id` | `String` | Custom emoji id to prefix the welcome message (MarkdownV2: ![x](tg://emoji?id=...)) |  | No |  |  |

## User Blocking

Redis-backed user blocking storage


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.telegram.block.key-prefix` | `String` | Redis key prefix for block entries |  | No |  |  |

## Channel Cache

Redis cache for Telegram channel API responses


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.telegram.channel.cache.chat-info-ttl` | `Duration` | TTL for cached chat info entries |  | No |  |  |
| `app.telegram.channel.cache.admins-ttl` | `Duration` | TTL for cached administrator list entries |  | No |  |  |
| `app.telegram.channel.cache.key-prefix` | `String` | Redis key prefix for channel cache |  | No |  |  |

## Update Deduplication

Deduplication of incoming Telegram updates via Redis


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.telegram.deduplication.ttl` | `Duration` | TTL for processed update ids in Redis |  | No |  |  |

## Telegram Resilience

Circuit breaker and bulkhead settings for Telegram API


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.telegram.resilience.circuit-breaker.sliding-window-size` | `Positive int` | Count-based sliding window size |  | No |  |  |
| `app.telegram.resilience.circuit-breaker.failure-rate-threshold` | `Positive int` | Failure rate percentage to open circuit |  | No |  | `40`, `60` |
| `app.telegram.resilience.circuit-breaker.slow-call-duration` | `Duration` | Threshold duration for slow calls |  | No |  |  |
| `app.telegram.resilience.circuit-breaker.wait-in-open-state` | `Duration` | Wait duration in open state before half-open |  | No |  |  |
| `app.telegram.resilience.circuit-breaker.half-open-calls` | `Positive int` | Permitted calls in half-open state |  | No |  |  |
| `app.telegram.resilience.circuit-breaker.minimum-calls` | `Positive int` | Minimum calls before evaluating failure rate |  | No |  |  |
| `app.telegram.resilience.bulkhead.max-concurrent-calls` | `Positive int` | Max concurrent Telegram API calls |  | No |  |  |
| `app.telegram.resilience.bulkhead.max-wait-duration` | `Duration` | Max wait duration for a bulkhead permit |  | No |  |  |

## Telegram Retry

Retry behaviour for Telegram API calls


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.telegram.retry.max-attempts` | `Positive int` | Max retry attempts including initial call |  | No |  |  |
| `app.telegram.retry.backoff-intervals` | `Duration>` | Backoff durations between retries |  | No |  |  |

## Telegram Sender

Rate limiting and caching for the Telegram message sender


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.telegram.sender.global-per-sec` | `Positive int` | Global rate limit messages/sec |  | No |  |  |
| `app.telegram.sender.per-chat-per-sec` | `Positive int` | Per-chat rate limit messages/sec |  | No |  |  |
| `app.telegram.sender.cache-expire-after-access` | `Duration` | Per-chat semaphore cache expiry duration |  | No |  |  |
| `app.telegram.sender.cache-maximum-size` | `Positive int` | Max per-chat semaphore cache entries |  | No |  |  |
| `app.telegram.sender.replenish-fixed-rate-ms` | `Positive long` | Semaphore replenish interval in milliseconds |  | No |  |  |

## User State

Redis-backed user conversational state storage


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.telegram.state.default-ttl` | `Duration` | Default TTL for user state entries |  | No |  |  |
| `app.telegram.state.key-prefix` | `String` | Redis key prefix for state entries |  | No |  |  |
