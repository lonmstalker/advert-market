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
  "text_requirements": "Promotional post about a crypto wallet. Mention security and ease of use.",
  "tone": "CASUAL",
  "required_links": [
    {"url": "https://example.com/wallet", "label": "Download"}
  ],
  "required_media": [
    {"type": "PHOTO", "file_id": "AgACAgIAAxkBAAI...", "description": "Application logo"}
  ],
  "additional_notes": "Do not mention competitors"
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
  "text": "<b>Secure crypto wallet</b>\n\nA simple and reliable way to store your TON.\n\n✅ Biometric security\n✅ Instant transfers\n✅ NFT support",
  "media": [
    {"type": "PHOTO", "file_id": "AgACAgIAAxkBAAI...", "caption": null}
  ],
  "buttons": [
    {"text": "Download", "url": "https://example.com/wallet"}
  ],
  "parse_mode": "HTML",
  "version": 2,
  "revision_note": "Added benefits at advertiser request"
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