# Delivery Verifier -- Telegram API Details

## Overview

Delivery Verifier checks that published ads remain intact in the channel for 24 hours. Uses Telegram Bot API to read messages and compare content hashes.

---

## Telegram Bot API Methods

### getChat

Verify channel accessibility before checking messages.

```
GET /bot{token}/getChat?chat_id={channel_id}
```

Returns channel info. If bot removed -> 403 Forbidden.

### forwardMessage (for content reading)

Forward message to a private chat (bot's own) to read content:

```json
POST /bot{token}/forwardMessage
{
  "chat_id": "{bot_private_chat_id}",
  "from_chat_id": "{channel_id}",
  "message_id": "{message_id}"
}
```

If message deleted -> error 400 "Bad Request: message to forward not found".

### Alternative: getMessages (Bot API 7.x+)

If available, use `getMessages` to read message content directly without forwarding.

---

## Content Hash Algorithm

SHA-256 hash of normalized message content:

### Hash Input Construction

```
text_content = message.text (trimmed, normalized whitespace)
media_ids = sorted list of file_id for all media attachments
button_text = sorted list of button labels

hash_input = text_content + "\n" + join(media_ids, ",") + "\n" + join(button_text, ",")
content_hash = "sha256:" + SHA256(hash_input.getBytes(UTF_8)).toHex()
```

### Normalization Rules

1. Trim leading/trailing whitespace
2. Collapse multiple spaces/newlines into single space
3. Remove zero-width characters
4. Media `file_id` sorted alphabetically
5. Button labels sorted alphabetically

---

## Verification Schedule

| Check # | Time After Publication | Action |
|---------|----------------------|--------|
| 1 | +1 hour | First check |
| 2 | +4 hours | Second check |
| 3 | +8 hours | Third check |
| 4 | +16 hours | Fourth check |
| 5 | +24 hours | Final check (if all pass -> VERIFIED) |

### Retry on API Failure

Each check retried 3 times with exponential backoff (2s, 4s, 8s).
If all retries fail -> schedule recheck in 30 minutes.

### 24h Timeout with Unresolved Checks

If the 24h deadline is reached and verification checks could not complete (API errors, channel unreachable):
- **DO NOT auto-approve** (COMPLETED_RELEASED) — this would release escrow without verification
- **Transition to DISPUTED** with reason `VERIFICATION_INCONCLUSIVE`
- Operator manually resolves after investigating

> **Rationale**: Auto-approve on API error is a money-loss vector. Owner could delete post + block bot, and auto-approve would still release funds.

---

## Failure Detection

### Post Deleted

- `forwardMessage` returns error "message to forward not found"
- Action: Auto-open dispute with reason `POST_DELETED`

### Content Edited

- Content hash of current message != stored `content_hash`
- Action: Auto-open dispute with reason `CONTENT_EDITED`

### Partial Edit Detection

| Change | Severity | Action |
|--------|----------|--------|
| Text completely different | HIGH | CONTENT_EDITED dispute |
| Minor text change (< 5% diff) | MEDIUM | Log warning, escalate to operator |
| Media added/removed | HIGH | CONTENT_EDITED dispute |
| Buttons changed | MEDIUM | Log warning, continue |
| Formatting only (bold/italic) | LOW | Ignore |

### Channel Became Private

- `getChat` returns 403 or indicates private channel
- Action: Auto-open dispute with reason `CHANNEL_RESTRICTED`

---

## 24h Calculation

- Publication time stored as `published_at` in UTC
- 24h deadline: `published_at + 24 hours`
- All check times calculated relative to `published_at`, not current time
- No timezone adjustment needed (all UTC)

---

## Configuration

```yaml
delivery:
  verifier:
    check-schedule: [1h, 4h, 8h, 16h, 24h]
    retry-max: 3
    retry-backoff: 2s
    recheck-interval: 30m
    hash-algorithm: SHA-256
```

---

## Related Documents

- [Delivery Verification Feature](../03-feature-specs/05-delivery-verification.md)
- [Dispute Auto-Resolution](./16-dispute-auto-resolution.md)
- [Telegram Bot Framework](./02-telegram-bot-framework.md)