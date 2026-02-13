# Creative Brief & Draft JSONB Schemas

## Overview

Two JSONB columns in `deals` table store creative workflow data:
- `creative_brief` -- advertiser's requirements for the ad
- `creative_draft` -- channel owner's ad content for review

---

## creative_brief JSON Schema

```json
{
  "type": "object",
  "required": ["text_requirements"],
  "properties": {
    "text_requirements": {
      "type": "string",
      "maxLength": 2000,
      "description": "What the ad text should contain/convey"
    },
    "target_audience": {
      "type": "string",
      "maxLength": 500
    },
    "tone": {
      "type": "string",
      "enum": ["FORMAL", "CASUAL", "HUMOROUS", "INFORMATIVE", "URGENT"]
    },
    "required_links": {
      "type": "array",
      "maxItems": 5,
      "items": {
        "type": "object",
        "properties": {
          "url": {"type": "string", "format": "uri"},
          "label": {"type": "string", "maxLength": 50}
        }
      }
    },
    "required_media": {
      "type": "array",
      "maxItems": 10,
      "items": {
        "type": "object",
        "properties": {
          "type": {"enum": ["PHOTO", "VIDEO", "GIF", "DOCUMENT"]},
          "file_id": {"type": "string"},
          "description": {"type": "string", "maxLength": 200}
        }
      }
    },
    "forbidden_words": {
      "type": "array",
      "maxItems": 20,
      "items": {"type": "string", "maxLength": 50}
    },
    "additional_notes": {
      "type": "string",
      "maxLength": 1000
    }
  }
}
```

### Example

```json
{
  "text_requirements": "\u0420\u0435\u043a\u043b\u0430\u043c\u043d\u044b\u0439 \u043f\u043e\u0441\u0442 \u043e \u043a\u0440\u0438\u043f\u0442\u043e-\u043a\u043e\u0448\u0435\u043b\u044c\u043a\u0435. \u0423\u043f\u043e\u043c\u044f\u043d\u0443\u0442\u044c \u0431\u0435\u0437\u043e\u043f\u0430\u0441\u043d\u043e\u0441\u0442\u044c \u0438 \u043f\u0440\u043e\u0441\u0442\u043e\u0442\u0443 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u044f.",
  "tone": "CASUAL",
  "required_links": [
    {"url": "https://example.com/wallet", "label": "\u0421\u043a\u0430\u0447\u0430\u0442\u044c"}
  ],
  "required_media": [
    {"type": "PHOTO", "file_id": "AgACAgIAAxkBAAI...", "description": "\u041b\u043e\u0433\u043e\u0442\u0438\u043f \u043f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u044f"}
  ],
  "additional_notes": "\u041d\u0435 \u0443\u043f\u043e\u043c\u0438\u043d\u0430\u0442\u044c \u043a\u043e\u043d\u043a\u0443\u0440\u0435\u043d\u0442\u043e\u0432"
}
```

---

## creative_draft JSON Schema

```json
{
  "type": "object",
  "required": ["text"],
  "properties": {
    "text": {
      "type": "string",
      "maxLength": 4096,
      "description": "Full ad text (HTML formatting allowed)"
    },
    "media": {
      "type": "array",
      "maxItems": 10,
      "items": {
        "type": "object",
        "required": ["type", "file_id"],
        "properties": {
          "type": {"enum": ["PHOTO", "VIDEO", "GIF", "DOCUMENT"]},
          "file_id": {"type": "string"},
          "caption": {"type": "string", "maxLength": 1024}
        }
      }
    },
    "buttons": {
      "type": "array",
      "maxItems": 5,
      "items": {
        "type": "object",
        "required": ["text", "url"],
        "properties": {
          "text": {"type": "string", "maxLength": 50},
          "url": {"type": "string", "format": "uri"}
        }
      }
    },
    "parse_mode": {
      "type": "string",
      "enum": ["HTML", "Markdown"],
      "default": "HTML"
    },
    "disable_web_page_preview": {
      "type": "boolean",
      "default": false
    },
    "version": {
      "type": "integer",
      "minimum": 1,
      "description": "Draft revision number"
    },
    "revision_note": {
      "type": "string",
      "maxLength": 500,
      "description": "What changed in this revision"
    }
  }
}
```

### Example

```json
{
  "text": "<b>\u0411\u0435\u0437\u043e\u043f\u0430\u0441\u043d\u044b\u0439 \u043a\u0440\u0438\u043f\u0442\u043e-\u043a\u043e\u0448\u0435\u043b\u0451\u043a</b>\n\n\u041f\u0440\u043e\u0441\u0442\u043e\u0439 \u0438 \u043d\u0430\u0434\u0451\u0436\u043d\u044b\u0439 \u0441\u043f\u043e\u0441\u043e\u0431 \u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0432\u0430\u0448\u0438 TON.\n\n✅ \u0411\u0438\u043e\u043c\u0435\u0442\u0440\u0438\u0447\u0435\u0441\u043a\u0430\u044f \u0437\u0430\u0449\u0438\u0442\u0430\n✅ \u041c\u0433\u043d\u043e\u0432\u0435\u043d\u043d\u044b\u0435 \u043f\u0435\u0440\u0435\u0432\u043e\u0434\u044b\n✅ \u041f\u043e\u0434\u0434\u0435\u0440\u0436\u043a\u0430 NFT",
  "media": [
    {"type": "PHOTO", "file_id": "AgACAgIAAxkBAAI...", "caption": null}
  ],
  "buttons": [
    {"text": "\u0421\u043a\u0430\u0447\u0430\u0442\u044c", "url": "https://example.com/wallet"}
  ],
  "parse_mode": "HTML",
  "version": 2,
  "revision_note": "\u0414\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u044b \u043f\u0440\u0435\u0438\u043c\u0443\u0449\u0435\u0441\u0442\u0432\u0430 \u043f\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u0443 \u0440\u0435\u043a\u043b\u0430\u043c\u043e\u0434\u0430\u0442\u0435\u043b\u044f"
}
```

---

## Validation Rules

| Field | Constraint |
|-------|-----------|
| `creative_brief.text_requirements` | Required, 1-2000 chars |
| `creative_draft.text` | Required, 1-4096 chars |
| Media count | Max 10 items per brief/draft |
| Buttons count | Max 5 per draft |
| Button text | Max 50 chars |
| Button URL | Valid URI format |
| Total media size | Validated by Telegram API limits |

---

## Version Tracking

- `creative_draft.version` starts at 1
- Incremented on each revision
- Previous versions stored in `deal_events` payload for audit trail
- `revision_note` describes what changed

### Revision Flow

```
1. Owner submits draft (version=1) -> CREATIVE_SUBMITTED
2. Advertiser requests revision -> REVISION_REQUESTED
3. Owner submits revised draft (version=2) -> CREATIVE_SUBMITTED
4. Advertiser approves -> CREATIVE_APPROVED
```

---

## Related Documents

- [Creative Workflow Feature](../03-feature-specs/03-creative-workflow.md)
- [Deal Lifecycle](../03-feature-specs/02-deal-lifecycle.md)
- [Telegram Bot Framework](./02-telegram-bot-framework.md)