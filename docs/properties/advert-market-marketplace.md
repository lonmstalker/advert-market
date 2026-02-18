# advert-market-marketplace

## Table of Contents

- [Channel Bot](#channel-bot)
- [Channel Statistics Collector](#channel-statistics-collector)
- [Creative Storage](#creative-storage)
- [Team Management](#team-management)


---

## Channel Bot

Bot identity for channel admin verification


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.marketplace.channel.bot-user-id` | `Positive long` | Telegram user ID of the bot |  | Yes |  |  |
| `app.marketplace.channel.verification-timeout` | `Duration` | Timeout for Telegram API calls during channel verification |  | No |  |  |

## Channel Statistics Collector

Periodic Telegram subscriber sync for channels


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.marketplace.channel.statistics.enabled` | `boolean` | Enable periodic channel statistics collection |  | No |  |  |
| `app.marketplace.channel.statistics.batch-size` | `Positive int` | Maximum number of channels processed per cycle |  | No |  |  |
| `app.marketplace.channel.statistics.retry-backoff-ms` | `PositiveOrZero long` | Backoff in milliseconds between retries for transient Telegram failures |  | No |  |  |
| `app.marketplace.channel.statistics.max-retries-per-channel` | `PositiveOrZero int` | Maximum retries per channel for transient Telegram failures |  | No |  |  |
| `app.marketplace.channel.statistics.admin-check-interval` | `Duration` | Minimum interval between periodic admin list checks |  | No |  |  |
| `app.marketplace.channel.statistics.estimated-view-rate-bp` | `Max(10000L) int` | Estimated 24h views ratio in basis points for automatic avg_views/engagement_rate updates |  | No |  |  |

## Creative Storage

S3-compatible storage for creative media assets


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.marketplace.creatives.storage.enabled` | `boolean` | Enable S3-compatible storage adapter |  | No |  |  |
| `app.marketplace.creatives.storage.endpoint` | `String` | Custom S3 endpoint URL (use MinIO in development) |  | No |  |  |
| `app.marketplace.creatives.storage.region` | `String` | S3 region used by the client |  | No |  |  |
| `app.marketplace.creatives.storage.bucket` | `NotBlank String` | Bucket name for uploaded creative media |  | No |  |  |
| `app.marketplace.creatives.storage.access-key` | `String` | S3 access key (dev/prod credentials) |  | No |  |  |
| `app.marketplace.creatives.storage.secret-key` | `String` | S3 secret key (dev/prod credentials) |  | No |  |  |
| `app.marketplace.creatives.storage.public-base-url` | `String` | Public base URL returned to clients |  | No |  |  |
| `app.marketplace.creatives.storage.key-prefix` | `String` | Object key prefix under bucket |  | No |  |  |

## Team Management

Limits for channel team management


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.marketplace.team.max-managers` | `Positive int` | Maximum number of managers per channel |  | Yes |  |  |
