# 45. Channel Statistics Collector

## Overview

Background scheduled task для обновления статистики каналов (subscriber count, avg views, engagement rate) из Telegram API для поиска и отображения в маркетплейсе.

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

- `avg_views` — owner self-reports при регистрации канала
- Validation: `avg_views <= subscriber_count`

### Post-MVP

- TGStat API для автоматического `avg_views` и `engagement_rate`

## Collection Schedule

| Параметр | Значение |
|----------|----------|
| Interval | 6 часов |
| Batch size | 100 каналов за цикл |
| Rate limit | 1 req/sec к Telegram API |
| Приоритет | `stats_updated_at NULLS FIRST` (никогда не обновлявшиеся) |
| Stale threshold | 24h — получают приоритет |

## Freshness Policy

| Условие | Поведение |
|---------|-----------|
| `stats_updated_at < NOW() - 12h` | Badge "Обновлено X часов назад" |
| `stats_updated_at < NOW() - 24h` | Marked as stale |
| `stats_updated_at < NOW() - 7d` | Исключен из поиска по умолчанию |

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
    stale-threshold: 24h
    exclude-after: 7d
```

## Monitoring

| Metric | Type |
|--------|------|
| `marketplace.channel.stats.updates{status}` | Counter |
| `marketplace.channel.stats.age` | Timer |

## Related Documentation

- [29-channel-search-impl.md](./29-channel-search-impl.md)
- [02-telegram-bot-framework.md](./02-telegram-bot-framework.md)
- [21-metrics-slos-monitoring.md](./21-metrics-slos-monitoring.md)