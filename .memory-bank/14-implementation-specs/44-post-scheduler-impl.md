# 44. Post Scheduler Worker

## Overview

Worker consuming `PUBLISH_POST` from `delivery.commands` Kafka topic. Publishes approved advertising content to Telegram channels.

**Module**: `delivery-impl`

## Telegram API Send Logic

Selecting a method by `creative_draft.media[]`:

| Condition | Telegram method | Note |
|---------|---------------|------------|
| No media | `sendMessage` | text + InlineKeyboardMarkup |
| 1 photo | `sendPhoto` | caption + reply_markup |
| 1 video | `sendVideo` | caption + reply_markup |
| 1 document | `sendDocument` | caption + reply_markup |
| 1 animation | `sendAnimation` | caption + reply_markup |
| 2-10 media | `sendMediaGroup` + `sendMessage` | MediaGroup does not support InlineKeyboard - buttons with separate message |

**InlineKeyboardMarkup**: `creative_draft.buttons[]` → `InlineKeyboardButton.url(text, url)`.

## Scheduled Publication Flow

```
1. CREATIVE_APPROVED → SCHEDULED:
   - Deal.scheduled_at = \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u043e\u0435 \u0432\u0440\u0435\u043c\u044f

2. Deal Timeout Scheduler (cron \u043a\u0430\u0436\u0434\u0443\u044e \u043c\u0438\u043d\u0443\u0442\u0443):
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

Caption only applies to the first element in the MediaGroup (Telegram limitation).

## Error Handling

| Error | HTTP Code | Recovery |
|-------|-----------|----------|
| Bot not in channel | 403 | Alert operator, DLQ |
| Rate limit | 429 | Retry with `Retry-After` header (max 3) |
| File_id expired | 400 | Notify owner, pause deal |
| MediaGroup partial failure | 400 | Retry entire group atomically |
| Channel restricted | 403 | Alert operator |

Retry: max 3, exponential backoff 1s→2s→4s. Non-retryable (403, invalid file_id) → DLQ.

## Idempotency

**Key**: `publish:{deal_id}`

Verification is performed through the uniqueness of the business key `publish:{deal_id}` and optimistic locking for the transaction. After success, `message_id` is saved, republishing for the same key returns idempotent no-op.

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
