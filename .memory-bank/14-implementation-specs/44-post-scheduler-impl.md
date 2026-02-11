# 44. Post Scheduler Worker

## Overview

Worker, потребляющий `PUBLISH_POST` из `delivery.commands` Kafka topic. Публикует одобренный рекламный контент в Telegram-каналы.

**Module**: `delivery-impl`

## Telegram API Send Logic

Выбор метода по `creative_draft.media[]`:

| Условие | Telegram метод | Примечание |
|---------|---------------|------------|
| Нет медиа | `sendMessage` | text + InlineKeyboardMarkup |
| 1 photo | `sendPhoto` | caption + reply_markup |
| 1 video | `sendVideo` | caption + reply_markup |
| 1 document | `sendDocument` | caption + reply_markup |
| 1 animation | `sendAnimation` | caption + reply_markup |
| 2-10 media | `sendMediaGroup` + `sendMessage` | MediaGroup не поддерживает InlineKeyboard — кнопки отдельным сообщением |

**InlineKeyboardMarkup**: `creative_draft.buttons[]` → `InlineKeyboardButton.url(text, url)`.

## Scheduled Publication Flow

```
1. CREATIVE_APPROVED → SCHEDULED:
   - Deal.scheduled_at = выбранное время

2. Deal Timeout Scheduler (cron каждую минуту):
   SELECT deal_id FROM deals
   WHERE status = 'SCHEDULED' AND scheduled_at <= NOW()
   FOR UPDATE SKIP LOCKED
   → Emit PUBLISH_POST via transactional outbox

3. Post Scheduler Worker:
   - Consume PUBLISH_POST
   - Publish via Telegram API
   - Store message_id in deals.published_message_id
   - Transition → PUBLISHED
```

## Content Mapping

| JSONB Field | Telegram Param | Notes |
|-------------|----------------|-------|
| `text` | `text` / `caption` | HTML-escaped |
| `parse_mode` | `parse_mode` | Default: `HTML` |
| `media[].type` | Method selection | `photo`, `video`, `document`, `animation` |
| `media[].file_id` | `file_id` | Telegram file_id from upload |
| `buttons[]` | `reply_markup` | InlineKeyboardMarkup |
| `disable_web_page_preview` | `disable_web_page_preview` | Boolean, default `false` |

Caption применяется только к первому элементу в MediaGroup (ограничение Telegram).

## Error Handling

| Error | HTTP Code | Recovery |
|-------|-----------|----------|
| Bot not in channel | 403 | Alert operator, DLQ |
| Rate limit | 429 | Retry с `Retry-After` header (max 3) |
| File_id expired | 400 | Notify owner, pause deal |
| MediaGroup partial failure | 400 | Retry entire group atomically |
| Channel restricted | 403 | Alert operator |

Retry: max 3, exponential backoff 1s→2s→4s. Non-retryable (403, invalid file_id) → DLQ.

## Idempotency

**Key**: `publish:{deal_id}`

Проверка `processed_events` перед отправкой. После успеха — сохранение `message_id` и запись в `processed_events`.

## Configuration

```yaml
delivery:
  post-scheduler:
    consumer:
      group-id: post-scheduler-worker
      topic: delivery.commands
      concurrency: 4
    send-timeout: 30s
    rate-limit:
      per-channel:
        rate: 1
        period: 3s
      global:
        rate: 30
        period: 1s
    retry:
      max-attempts: 3
      backoff-initial: 1s
      backoff-multiplier: 2
```

Rate limiter: Redis sorted sets, token bucket algorithm.

## Monitoring

| Metric | Type |
|--------|------|
| `delivery.post_scheduler.published.total{media_type}` | Counter |
| `delivery.post_scheduler.errors.total{error_type}` | Counter |
| `delivery.post_scheduler.send.duration{media_type}` | Timer |

## Related Specifications

- [20-creative-jsonb-schemas.md](./20-creative-jsonb-schemas.md)
- [04-kafka-event-schemas.md](./04-kafka-event-schemas.md)
- [35-deal-workflow-engine.md](./35-deal-workflow-engine.md)
- [19-deal-timeout-scheduler.md](./19-deal-timeout-scheduler.md)
- [32-worker-monitoring-dlq.md](./32-worker-monitoring-dlq.md)