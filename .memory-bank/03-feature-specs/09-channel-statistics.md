# Feature: Channel Statistics Verification

## Overview

Channel statistics (subscriber_count, avg_views, engagement_rate) displayed to advertisers must be accurate and fresh. This feature defines how statistics are collected, verified, and refreshed from Telegram.

## Statistics Fields

| Field | Source | Update Frequency |
|-------|--------|:---------------:|
| `subscriber_count` | Telegram Bot API: `getChatMemberCount` | On listing view + daily refresh |
| `avg_views` | Owner-reported (MVP) / TDLib (Scaled) | Daily refresh |
| `engagement_rate` | Calculated: avg_views / subscriber_count | On avg_views update |
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

### getChat

```
GET /bot{token}/getChat?chat_id={channel_id}
```

Returns: chat title, description, linked_chat_id.

### Average Views (MVP Limitation)

Telegram Bot API does not expose view counts. MVP strategy:

- Owner self-reports `avg_views`
- Platform displays "owner-reported" badge for unverified stats
- `stats_source` field: `OWNER_REPORTED` | `BOT_VERIFIED` | `UNVERIFIED`

Scaled: use Telegram Statistics API (TDLib/MTProto, channels with 500+ subscribers).

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
| Bot is channel admin | `getChatMember(channel_id, bot_id)` | Mark "unverified stats" |
| Subscriber count > 0 | API returns > 0 | Deactivate listing |
| Channel exists | `getChat` succeeds | Deactivate listing, notify owner |
| Owner still owns channel | Creator matches owner_id | Alert, ownership verification |

## Database Schema Addition

```sql
ALTER TABLE channels ADD COLUMN stats_updated_at TIMESTAMPTZ;
ALTER TABLE channels ADD COLUMN stats_source VARCHAR(20) DEFAULT 'OWNER_REPORTED';
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/channels/{id}/refresh-stats` | Trigger manual stats refresh (owner) |
| `GET` | `/api/v1/channels/{id}/stats` | Get current stats with freshness info |

## Configuration

```yaml
channel:
  stats:
    freshness-window: 1h
    deal-freshness-window: 15m
    batch-refresh-schedule: "0 0 3 * * *"  # 3 AM UTC daily
    batch-size: 100
    batch-delay-ms: 1000
    rate-limit-per-sec: 25
```

## Components Involved

| Component | Role |
|-----------|------|
| Channel Service | Stats CRUD, freshness checks |
| Telegram Bot API client | `getChatMemberCount`, `getChat` calls |
| StatsRefreshScheduler | Daily batch refresh cron job |
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
- [Rate Limiting](../14-implementation-specs/27-rate-limiting-strategy.md)