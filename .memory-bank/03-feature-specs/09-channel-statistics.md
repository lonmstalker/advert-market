# Feature: Channel Statistics Verification

## Overview

Channel statistics (subscriber_count, avg_views, engagement_rate) displayed to advertisers must be accurate and fresh. This feature defines how statistics are collected, verified, and refreshed from Telegram.

## Statistics Fields

| Field | Source | Update Frequency |
|-------|--------|:---------------:|
| `subscriber_count` | Telegram Bot API: `getChatMemberCount` | On listing view + daily refresh |
| `avg_views` | Runtime estimate from `subscriber_count` using `estimated-view-rate-bp` | Daily refresh |
| `engagement_rate` | Calculated: `avg_views / subscriber_count` (rounded to 2 decimals, %) | On avg_views update |
| `topic` | Owner-provided | On registration |

## Collection Mechanism

### On-Demand (Lazy) Refresh

When an advertiser views a channel listing:

1. Check `channels.stats_updated_at`
2. If stale (> configured freshness window, default 1h): trigger async refresh
3. Return cached stats immediately (non-blocking)
4. Background job fetches fresh stats from Telegram API
5. Update `channels` table on completion

### Scheduled (Batch) Refresh

Daily cron job refreshes stats for all active channels:

1. Query: `SELECT id FROM channels WHERE is_active = true ORDER BY stats_updated_at ASC`
2. Rate-limited calls to Telegram API (25 req/sec, below 30/sec limit)
3. Batch size: 100 channels, 1-second delay between batches
4. Update each channel's statistics and `stats_updated_at`

## Telegram API Calls

### getChatMemberCount

```
GET /bot{token}/getChatMemberCount?chat_id={channel_id}
```

Returns: integer (subscriber count). Bot must be admin of the channel.

Verification/runtime note:
- Channel verification performs Telegram reads sequentially and retries once after ~1.1s when local per-channel limiter returns `RATE_LIMIT_EXCEEDED`.

### getChat

```
GET /bot{token}/getChat?chat_id={channel_id}
```

Returns: chat title, description, linked_chat_id.

### Average Views (Current Runtime Behavior)

Telegram Bot API does not expose post view counts. Current runtime strategy:

- `avg_views = floor(subscriber_count * estimated_view_rate_bp / 10000)`
- default `estimated_view_rate_bp = 1200` (12.00%)
- `engagement_rate = round(avg_views * 100 / subscriber_count, 2)`
- no manual stats input from channel owners

Future enhancement: replace heuristics with external analytics providers
(e.g. TDLib/MTProto/TGStat) where available.

## Freshness Guarantees

| Context | Max Staleness | Trigger |
|---------|:------------:|---------|
| Channel listing page | 1 hour | Lazy refresh |
| Deal creation | 15 minutes | Force refresh before deal |
| Search results | 4 hours | Batch refresh only |
| Admin dashboard | Real-time | Direct API call |

## Verification Rules

| Rule | Check | Action on Failure |
|------|-------|-------------------|
| Bot is channel admin | `getChatAdministrators(channel_id)` contains `bot_id` with posting rights | Deactivate listing, notify owner |
| Subscriber count > 0 | API returns > 0 | Deactivate listing |
| Channel exists | `getChat` succeeds | Deactivate listing, notify owner |
| Owner still owns channel | owner remains in admins list | Deactivate listing, notify owner |

## Database Schema Addition

```sql
ALTER TABLE channels ADD COLUMN stats_updated_at TIMESTAMPTZ;
ALTER TABLE channels ADD COLUMN avg_views INTEGER DEFAULT 0;
ALTER TABLE channels ADD COLUMN engagement_rate DECIMAL(5,2) DEFAULT 0.00;
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/channels/{id}/refresh-stats` | Trigger manual stats refresh (owner) |
| `GET` | `/api/v1/channels/{id}/stats` | Get current stats with freshness info |

## Configuration

```yaml
app:
  marketplace:
    channel:
      statistics:
        enabled: true
        update-interval: 6h
        batch-size: 100
        retry-backoff-ms: 1000
        max-retries-per-channel: 2
        admin-check-interval: 24h
        estimated-view-rate-bp: 1200
```

## Components Involved

| Component | Role |
|-----------|------|
| Channel Service | Stats CRUD, freshness checks |
| Telegram Bot API client | `getChatMemberCount`, `getChat`, `getChatAdministrators` calls |
| ChannelStatisticsCollectorScheduler | Scheduled refresh + admin safety checks |
| Search Service | Returns stats with freshness indicator |

## Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `channel.stats.refresh` | Counter | Refresh attempts (by source: lazy/batch/manual) |
| `channel.stats.refresh.failed` | Counter | Failed refreshes (by reason) |
| `channel.stats.staleness` | Histogram | Age of stats at query time |

## Related Documents

- [Channel Marketplace](./01-channel-marketplace.md)
- [Backend API Components](../04-architecture/03-backend-api-components.md)
- [Telegram Bot Framework](../14-implementation-specs/02-telegram-bot-framework.md)
- [Channel Statistics Collector](../14-implementation-specs/45-channel-statistics-collector.md)
- [Rate Limiting](../14-implementation-specs/27-rate-limiting-strategy.md)
