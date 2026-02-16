# 45. Channel Statistics Collector

## Overview

Background scheduled task to update channel statistics (subscriber count, avg views, engagement rate) from the Telegram API for search and display in the marketplace.

**Module**: `marketplace-impl`

## DDL Extension

```sql
ALTER TABLE channels
  ADD COLUMN avg_views INTEGER DEFAULT 0,
  ADD COLUMN engagement_rate DECIMAL(5,2) DEFAULT 0.00,
  ADD COLUMN stats_updated_at TIMESTAMPTZ;

CREATE INDEX idx_channels_stats_updated
  ON channels (stats_updated_at NULLS FIRST)
  WHERE is_active = true;
```

## Data Sources

### Telegram API (auto)

- `getChatMemberCount` → `channels.subscriber_count` + `stats_updated_at`

### Manual Input (MVP)

- `avg_views` — owner self-reports when registering a channel
- Validation: `avg_views <= subscriber_count`

### Post-MVP

- TGStat API for automatic `avg_views` and `engagement_rate`

## Collection Schedule

| Parameter | Meaning |
|----------|----------|
| Interval | 6 hours |
| Batch size | 100 channels per cycle |
| Rate limit | 1 req/sec to Telegram API |
| Priority | `stats_updated_at NULLS FIRST` (never updated) |
| Stale threshold | 24h - get priority |

## Freshness Policy

| Condition | Behavior |
|---------|-----------|
| `stats_updated_at < NOW() - 12h` | Badge "Updated X hours ago" |
| `stats_updated_at < NOW() - 24h` | Marked as stale |
| `stats_updated_at < NOW() - 7d` | Excluded from search by default |

## Error Handling

| Error | Action |
|-------|--------|
| Channel not found (400) | `is_active = false` |
| Rate limit (429) | Exponential backoff, skip to next |
| Timeout | Retry on next cycle |
| 10+ consecutive failures | Disable auto-update for channel |

## Configuration

```yaml
marketplace:
  statistics:
    update-interval: 6h
    batch-size: 100
    retry-backoff-ms: 1000
    max-retries-per-channel: 2
    stale-threshold: 24h
    exclude-after: 7d
```

## Hardening Notes (2026-02-16)

- Collector execution is cycle-timed and channel processing is isolated per channel
  (no single long transaction across the whole batch).
- Transient failures (`SERVICE_UNAVAILABLE`, `RATE_LIMIT_EXCEEDED`) are retried with fixed backoff.
- Deactivation behavior for `CHANNEL_NOT_FOUND` and `CHANNEL_BOT_NOT_MEMBER` is preserved.
- Added operational metrics:
  - `channel.stats.collector.cycle.duration`
  - `channel.stats.collector.batch.size`
  - `channel.stats.collector.success`
  - `channel.stats.collector.failure`
  - `channel.stats.collector.retry{reason}`

## Monitoring

| Metric | Type |
|--------|------|
| `marketplace.channel.stats.updates{status}` | Counter |
| `marketplace.channel.stats.age` | Timer |

## Related Documentation

- [29-channel-search-impl.md](./29-channel-search-impl.md)
- [02-telegram-bot-framework.md](./02-telegram-bot-framework.md)
- [21-metrics-slos-monitoring.md](./21-metrics-slos-monitoring.md)
