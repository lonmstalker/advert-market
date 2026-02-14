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
| `app.telegram.bot-token` | `NotBlank String` | Telegram bot token from BotFather |  | Yes | NotBlank(must not be blank) | `123456:ABC-DEF...` |
| `app.telegram.bot-username` | `NotBlank String` | Telegram bot username |  | Yes | NotBlank(must not be blank) |  |
| `app.telegram.webhook` | `Valid Webhook` | Webhook configuration |  | No |  |  |
| `app.telegram.webapp` | `Valid WebApp` | Telegram Web App configuration |  | Yes |  |  |
| `app.telegram.welcome` | `Valid Welcome` | Welcome message configuration |  | No |  |  |

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
| `app.telegram.resilience.circuit-breaker` | `Valid CircuitBreaker` | Circuit breaker settings |  | No |  |  |
| `app.telegram.resilience.bulkhead` | `Valid Bulkhead` | Bulkhead settings for concurrent call limiting |  | No |  |  |

## Telegram Retry

Retry behaviour for Telegram API calls


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.telegram.retry.max-attempts` | `Positive int` | Max retry attempts including initial call |  | No | Positive(must be positive) |  |
| `app.telegram.retry.backoff-intervals` | `Duration>` | Backoff durations between retries |  | No |  |  |

## Telegram Sender

Rate limiting and caching for the Telegram message sender


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.telegram.sender.global-per-sec` | `Positive int` | Global rate limit messages/sec |  | No | Positive(must be positive) |  |
| `app.telegram.sender.per-chat-per-sec` | `Positive int` | Per-chat rate limit messages/sec |  | No | Positive(must be positive) |  |
| `app.telegram.sender.cache-expire-after-access` | `Duration` | Per-chat semaphore cache expiry duration |  | No |  |  |
| `app.telegram.sender.cache-maximum-size` | `Positive int` | Max per-chat semaphore cache entries |  | No | Positive(must be positive) |  |
| `app.telegram.sender.replenish-fixed-rate-ms` | `Positive long` | Semaphore replenish interval in milliseconds |  | No | Positive(must be positive) |  |

## User State

Redis-backed user conversational state storage


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.telegram.state.default-ttl` | `Duration` | Default TTL for user state entries |  | No |  |  |
| `app.telegram.state.key-prefix` | `String` | Redis key prefix for state entries |  | No |  |  |
