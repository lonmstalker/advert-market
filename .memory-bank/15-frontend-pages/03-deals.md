#Deals

> Tab 2. Central module - complete deal flow from offer to completion/dispute.

## Navigation

```
/deals
  └── /deals/:dealId
      ├── /deals/:dealId/negotiate
      ├── /deals/:dealId/brief
      ├── /deals/:dealId/creative
      ├── /deals/:dealId/creative/review
      ├── /deals/:dealId/schedule
      ├── [Sheet] \u041e\u043f\u043b\u0430\u0442\u0430 (TON Connect)
      ├── [Sheet] \u041f\u043e\u0434\u0434\u0435\u0440\u0436\u043a\u0430
      ├── /deals/:dealId/dispute
      └── /deals/:dealId/dispute/evidence
```

---

## 3.1 List of deals

| | |
|---|---|
| **Route** | `/deals` |
| **Target** | All transactions of the user - as an advertiser and as a channel owner |
| **Who sees** | All authorized |

### API

```
GET /api/v1/deals?role=advertiser&cursor=&limit=20
GET /api/v1/deals?role=channel&cursor=&limit=20
```

**Query keys:** `dealKeys.list({ role: 'advertiser', ... })`, `dealKeys.list({ role: 'channel', ... })`

### UI

- **Segment-control**: `t('deals.list.asAdvertiser')` / `t('deals.list.asChannel')`
  - Visible only if the user has transactions in both roles
- **List of deals** — `Group` + `GroupItem`:
  - `before`: channel avatar (40×40)
  - Title: channel name
  - `subtitle`: post type
  - `after`: status badge (color)
- **Infinite scroll** — skeleton loading
- **Sort** by `updatedAt` (desc)

### ABAC (for the "Like channel" tab)

The manager sees channel transactions **only** with the `view_deals` right. Without this right, the “As a channel” tab is hidden.

### Status-badges

| Group | Statuses | Color |
|--------|---------|------|
| New | `DRAFT`, `OFFER_PENDING` | `accent` |
| In progress | `NEGOTIATING`, `ACCEPTED`, `AWAITING_PAYMENT`, `FUNDED` | `accent` (muted) |
| Creative | `CREATIVE_SUBMITTED`, `CREATIVE_APPROVED`, `SCHEDULED` | `accent` |
| Delivery | `PUBLISHED`, `DELIVERY_VERIFYING` | `warning` |
| Completed | `COMPLETED_RELEASED` | `success` |
| Problems | `DISPUTED` | `destructive` |
| Inactive | `CANCELLED`, `REFUNDED`, `EXPIRED` | `secondary` |

**Exhaustive mapping** — `Record<DealStatus, StatusConfig>`, compile-time check.

### Actions

| Action | Result |
|----------|-----------|
| Segment switching | Switching between lists |
| Tap on deal | → `/deals/:dealId` |
| Pull-to-refresh | Invalidate `dealKeys.lists()` |

### Empty states

| Role | Emoji | i18n title | i18n description | CTA |
|------|-------|------------|------------------|-----|
| Advertiser | `📬` | `deals.empty.advertiser.title` | `deals.empty.advertiser.description` | `deals.empty.advertiser.cta` → `/catalog` |
| Channel | `📬` | `deals.empty.channel.title` | `deals.empty.channel.description` | `deals.empty.channel.cta` → `/profile/channels/new` |

### Error states

| Error | UI |
|--------|----|
| Loading Error | `ErrorScreen` + retry |
| Offline | Banner `t('errors.offline')` |

---

## 3.2 Transaction details

| | |
|---|---|
| **Route** | `/deals/:dealId` |
| **Target** | Central deal screen - status, actions, timeline |
| **Who sees** | Advertiser or channel owner/manager of this deal (`view_deals`) |

### API

```
GET /api/v1/deals/:dealId
GET /api/v1/deals/:dealId/timeline
GET /api/v1/deals/:dealId/escrow     # \u0414\u043b\u044f funded-\u0441\u0442\u0430\u0442\u0443\u0441\u043e\u0432
```

**Query keys:** `dealKeys.detail(dealId)`, `dealKeys.timeline(dealId)`, `dealKeys.escrow(dealId)`

**Polling:** adaptive based on status:
- `AWAITING_PAYMENT`, `DELIVERY_VERIFYING`: 10s
- `PUBLISHED`: 30s
- Others: manual refetch

### UI

- **Header row:**
  - **Status-badge** – large, at the top
  - **ShareButton** — deep link `t.me/AdvertMarketBot/app?startapp=deal_{dealId_short}` (see 6.4)
- **Channel card** — compact, tap → `/catalog/channels/:channelId`
- **Amount** — `title2`, bold, `tabular-nums`, `<Amount>`
- **Action block** - depends on the role and status (matrix below)
- **Group `t('deals.detail.brief')`** — if available, collapsible
- **Group `t('deals.detail.creative')`** — if available, text preview + media thumbnails
- **Group `t('deals.detail.escrow')`** — escrow status, balance (for funded statuses)
- **Group `t('deals.detail.timeline')`** — chronological list of events
- **Button `t('deals.detail.support')`** (`secondary`, small) - opens the Support Sheet

### Action Matrix

| Status | Advertiser | Owner | Manager (required right) |
|--------|--------------|-------|--------------------------|
| `DRAFT` | — | — | — |
| `OFFER_PENDING` | [Cancel] `secondary destructive` | [Accept] `primary` / [Negotiation] `secondary` → 3.3 / [Reject] `secondary destructive` | `moderate`: same as Owner |
| `NEGOTIATING` | [Reply] `secondary` → 3.3 / [Cancel] `secondary destructive` | [Reply] `secondary` → 3.3 / [Reject] `secondary destructive` | `moderate`: same as Owner |
| `ACCEPTED` | — (waiting for payment) | [Cancel] `secondary destructive` | — |
| `AWAITING_PAYMENT` | [Pay] `primary` → Sheet 3.8 | — (waiting for payment) | — |
| `FUNDED` | [Send brief] `primary` → 3.4 | [Send creative] `primary` → 3.5 (if there is a brief) | `moderate`: same as Owner |
| `CREATIVE_SUBMITTED` | [Approve] `primary` → 3.6 / [Revision] `secondary` → 3.6 | — (waiting for review) | — |
| `CREATIVE_APPROVED` | — | [Publish] `primary` / [Schedule] `secondary` → 3.7 | `publish`: same as Owner |
| `SCHEDULED` | — | — | — |
| `PUBLISHED` | — | — | — |
| `DELIVERY_VERIFYING` | — | — | — |
| `COMPLETED_RELEASED` | Leave a review (v2) | — | — |
| `DISPUTED` | [Add evidence] `secondary` → 3.11 | [Add evidence] `secondary` → 3.11 | `view_deals`: same |
| `CANCELLED` | — | — | — |
| `REFUNDED` | — | — | — |
| `EXPIRED` | — | — | — |

**Implementation:** exhaustive `switch` with `default: never`.

### Role Definition

```typescript
type DealRole = 'advertiser' | 'channel_owner' | 'channel_manager';

function getDealRole(deal: Deal, userId: number): DealRole {
  if (deal.advertiserId === userId) return 'advertiser';
  // channel membership check from deal data
}
```

### Action API calls

| Action | Endpoint |
|----------|----------|
| Accept | `POST /api/v1/deals/:id/accept` |
| Reject | `POST /api/v1/deals/:id/reject` |
| Cancel | `POST /api/v1/deals/:id/cancel` |
| Approve creative | `POST /api/v1/deals/:id/creative/approve` |
| Publish | `POST /api/v1/deals/:id/publish` |

Destructive actions (cancellation, rejection) require `DialogModal` confirmation.

### Error states

| Error | UI |
|--------|----|
| 404 deal not found | `ErrorScreen` `t('errors.notFound.title')` + navigate `/deals` |
| 403 no access | `ErrorScreen` `t('errors.forbidden.title')` |
| 409 status changed | Toast `t('errors.conflict')` + auto-refetch |

---

## 3.3 Negotiations

| | |
|---|---|
| **Route** | `/deals/:dealId/negotiate` |
| **Target** | Submit a price counter-offer |
| **Who sees** | Advertiser or Owner/Manager (`moderate`) in the status `OFFER_PENDING` / `NEGOTIATING` |

### API

```
GET  /api/v1/deals/:dealId              # \u0422\u0435\u043a\u0443\u0449\u0438\u0435 \u0443\u0441\u043b\u043e\u0432\u0438\u044f
POST /api/v1/deals/:dealId/negotiate     # \u041a\u043e\u043d\u0442\u0440-\u043f\u0440\u0435\u0434\u043b\u043e\u0436\u0435\u043d\u0438\u0435
```

### UI

- **Current conditions** — read-only card: post type + current price (`<Amount>`)
- **Input `t('deals.negotiate.price')`** — numeric, TON, `<Amount>` format
- **Input `t('deals.negotiate.comment')`** — `textarea`, optional, max 2000 characters
- Button `t('deals.negotiate.submit')` (`primary`)

### Request body

```typescript
{
  proposedAmountNano: bigint;  // > 0
  pricingRuleId?: number;      // \u043e\u043f\u0446\u0438\u043e\u043d\u0430\u043b\u044c\u043d\u043e: \u0441\u043c\u0435\u043d\u0438\u0442\u044c \u0442\u0438\u043f \u043f\u043e\u0441\u0442\u0430
  message?: string;            // max 2000
}
```

### Actions

| Action | Result |
|----------|-----------|
| "Submit" | `POST /api/v1/deals/:id/negotiate` → navigate back to `/deals/:dealId` |

### ABAC

Manager: `moderate` required.

### Error states

| Error | UI |
|--------|----|
| Submit Error | Toast `t('common.toast.saveFailed')` |
| 409 status changed | Toast `t('errors.conflict')` + auto-refetch + navigate `/deals/:dealId` |

---

## 3.4 Sending a brief

| | |
|---|---|
| **Route** | `/deals/:dealId/brief` |
| **Target** | Advertiser describes creative requirements |
| **Who sees** | Advertiser with status `FUNDED` |

### API

```
GET  /api/v1/deals/:dealId        # \u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0441\u0442\u0430\u0442\u0443\u0441\u0430
POST /api/v1/deals/:dealId/brief  # \u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u0431\u0440\u0438\u0444\u0430
```

**Query keys:** `creativeKeys.brief(dealId)`

### UI

- Header: `t('deals.brief.title')`
- **Input `t('deals.brief.text')`** — `textarea`, placeholder: `t('deals.brief.textPlaceholder')`
- **Input `t('deals.brief.cta')`** — URL input
- **Input `t('deals.brief.restrictions')`** — `textarea`, placeholder: `t('deals.brief.restrictionsPlaceholder')`
- **Select `t('deals.brief.tone')`** — `t('deals.brief.tone.professional')` / `t('deals.brief.tone.informal')` / `t('deals.brief.tone.neutral')`
- **Uploading files** - examples, references (drag & drop or file picker)
- Button `t('deals.brief.submit')` (`primary`)

### Actions

| Action | Result |
|----------|-----------|
| "Submit" | `POST /api/v1/deals/:id/brief` → navigate `/deals/:dealId` |

### Error states

| Error | UI |
|--------|----|
| Error sending brief | Toast `t('common.toast.saveFailed')` |
| 409 status changed | Toast `t('errors.conflict')` + auto-refetch |

---

## 3.5 Submitting creative

| | |
|---|---|
| **Route** | `/deals/:dealId/creative` |
| **Target** | The channel owner creates a draft post according to the brief |
| **Who sees** | Owner/Manager (`moderate`) in status `FUNDED` |

### API

```
GET  /api/v1/deals/:dealId/brief      # \u0411\u0440\u0438\u0444 \u043e\u0442 \u0440\u0435\u043a\u043b\u0430\u043c\u043e\u0434\u0430\u0442\u0435\u043b\u044f
GET  /api/v1/deals/:dealId            # \u0421\u0442\u0430\u0442\u0443\u0441
POST /api/v1/deals/:dealId/creative   # \u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u043a\u0440\u0435\u0430\u0442\u0438\u0432\u0430
```

**Query keys:** `creativeKeys.brief(dealId)`, `creativeKeys.current(dealId)`

### UI

- **Group `t('deals.creative.brief')`** — read-only, data from the advertiser (collapsible)
- **Button `t('deals.creative.importFromTelegram')`** (`secondary`, small) — import an existing post from a channel (see “Import from Telegram” below)
- **Input `t('deals.creative.text')`** — `textarea`, max 4096 characters (Telegram limit), character counter
- **Media upload** - up to 10 images, drag & drop, thumbnails grid
- **Button Builder** - optional:
  - Each button: Input `t('deals.creative.buttonText')` + Input `t('deals.creative.buttonUrl')`
  - Up to 3 rows of buttons
  - Button `t('deals.creative.addButton')` (`link`)
- **Preview** - imitation of a Telegram post (real-time update as you type)
- Button `t('deals.creative.submit')` (`primary`)

### Import from Telegram (MVP)

Flow of sending a post through a bot:

1. User presses `t('deals.creative.importFromTelegram')`
2. Mini App shows instructions: `t('deals.creative.importInstruction')` - "Forward the post to the bot @AdvertMarketBot"
3. Button `t('deals.creative.openBot')` → `openTelegramLink('https://t.me/AdvertMarketBot')`
4. The user forwards the post to the bot
5. The bot parses the post (text, media, buttons) → saves by `dealId`
6. Mini App polling `GET /api/v1/deals/:dealId/creative/import` (every 3s, timeout 60s)
7. Upon receipt—autofill the form

### ABAC

Manager: `moderate` required.

### Request body

```typescript
{
  text: string;                           // max 4096
  mediaUrls?: string[];                   // max 10
  buttons?: { text: string; url: string }[]; // max 9 (3×3)
  format?: 'STANDARD' | 'PINNED' | 'STORY' | 'REPOST' | 'NATIVE';
}
```

### Actions

| Action | Result |
|----------|-----------|
| Entering text | Real-time update preview |
| "Import from Telegram" | Instructions + polling → autocomplete |
| "Submit for review" | `POST /api/v1/deals/:id/creative` → navigate `/deals/:dealId` |

### Error states

| Error | UI |
|--------|----|
| Error sending creative | Toast `t('common.toast.saveFailed')` |
| Media loading error | Toast `t('deals.error.mediaUploadFailed')` |
| Import timeout from Telegram | Toast `t('deals.error.importTimeout')` + retry |
| 409 status changed | Toast `t('errors.conflict')` + auto-refetch |

---

## 3.6 Creative review

| | |
|---|---|
| **Route** | `/deals/:dealId/creative/review` |
| **Target** | The advertiser evaluates the draft and makes a decision |
| **Who sees** | **Advertiser only** in the status `CREATIVE_SUBMITTED` |

### API

```
GET  /api/v1/deals/:dealId/creative         # \u0422\u0435\u043a\u0443\u0449\u0438\u0439 \u0447\u0435\u0440\u043d\u043e\u0432\u0438\u043a
GET  /api/v1/deals/:dealId/brief            # \u0414\u043b\u044f \u0441\u0440\u0430\u0432\u043d\u0435\u043d\u0438\u044f
POST /api/v1/deals/:dealId/creative/approve  # \u041e\u0434\u043e\u0431\u0440\u0438\u0442\u044c
POST /api/v1/deals/:dealId/creative/revision # \u0417\u0430\u043f\u0440\u043e\u0441\u0438\u0442\u044c \u0440\u0435\u0432\u0438\u0437\u0438\u044e
```

**Query keys:** `creativeKeys.current(dealId)`, `creativeKeys.brief(dealId)`

### UI

- **Creative preview** - like in Telegram: text + media + buttons
- **Group `t('deals.review.brief')`** — read-only, for comparison (collapsible)
- **Input `t('deals.review.revisionComment')`** — `textarea`, appears when you click "Request revision"
- Two buttons:
  - `t('deals.review.requestRevision')` (`secondary`)
  - `t('deals.review.approve')` (`primary`)

### Actions

| Action | Result |
|----------|-----------|
| "Approve" | `POST /api/v1/deals/:id/creative/approve` → navigate `/deals/:dealId` |
| "Request a revision" | Show comment field → `POST /api/v1/deals/:id/creative/revision` → navigate `/deals/:dealId` |

### Error states

| Error | UI |
|--------|----|
| Approval/Revision Error | Toast `t('common.toast.saveFailed')` |
| 409 status changed | Toast `t('errors.conflict')` + auto-refetch |

---

## 3.7 Planning a publication

| | |
|---|---|
| **Route** | `/deals/:dealId/schedule` |
| **Target** | The channel owner chooses the publication time |
| **Who sees** | Owner/Manager (`publish`) in status `CREATIVE_APPROVED` |

### API

```
GET  /api/v1/deals/:dealId          # \u0421\u0442\u0430\u0442\u0443\u0441 + \u043a\u0440\u0435\u0430\u0442\u0438\u0432
POST /api/v1/deals/:dealId/publish   # \u041e\u043f\u0443\u0431\u043b\u0438\u043a\u043e\u0432\u0430\u0442\u044c \u0441\u0435\u0439\u0447\u0430\u0441
POST /api/v1/deals/:dealId/schedule  # \u0417\u0430\u043f\u043b\u0430\u043d\u0438\u0440\u043e\u0432\u0430\u0442\u044c
```

### UI

- **Creative preview** – compact
- **Date picker** — up to 30 days in advance, min = today
- **Time picker** — hour:minute
- **Timezone** — auto-detection, read-only (from `Intl.DateTimeFormat().resolvedOptions().timeZone`)
- Two buttons:
  - `t('deals.schedule.publishNow')` (`primary`)
  - `t('deals.schedule.schedule')` (`secondary`) - active only after selecting date/time

### Request body (schedule)

```typescript
{
  scheduledAt: string;  // ISO 8601, \u0432 \u0431\u0443\u0434\u0443\u0449\u0435\u043c, max 30 \u0434\u043d\u0435\u0439
}
```

### Actions

| Action | Result |
|----------|-----------|
| "Publish Now" | `POST /api/v1/deals/:id/publish` → navigate `/deals/:dealId` |
| "Schedule" | `POST /api/v1/deals/:id/schedule` → navigate `/deals/:dealId` |

### ABAC

Manager: `publish` required.

### Error states

| Error | UI |
|--------|----|
| Publishing/Scheduling Error | Toast `t('common.toast.saveFailed')` |
| 409 status changed | Toast `t('errors.conflict')` + auto-refetch |
| Date in the past | Inline error `t('deals.error.pastDate')` |

---

## 3.8 Payment (Sheet - TON Connect)

| | |
|---|---|
| **Route** | N/A (Sheet overlay on 3.2) |
| **Target** | Payment for a transaction via TON Connect |
| **Who sees** | **Advertiser only** in the status `AWAITING_PAYMENT` |

### API

```
GET /api/v1/deals/:dealId/deposit   # escrow address, amount
```

**Query keys:** `dealKeys.deposit(dealId)`

### UI

- **Amount** — hero, `tabular-nums`, `<Amount>`
- **Platform commission** — `caption`, `secondary` (10%)
- **Total** — `title2`, bold
- **Wallet status** — icon + address (truncated), if connected
- Button `t('wallet.connectWallet')` (`secondary`) - if not connected
- Button `t('deals.payment.pay')` (`primary`) - available after connection
- Text `caption`: `t('deals.payment.escrowNote')`

### Actions

| Action | Result |
|----------|-----------|
| "Connect wallet" | TON Connect flow (tonConnectUI.connectWallet()) |
| "Pay" | Sign the transaction → toast `t('wallet.toast.paymentSent')` → close sheet |

### TON Connect integration

```typescript
const transaction = {
  validUntil: Math.floor(Date.now() / 1000) + 600, // 10 min
  messages: [{
    address: depositData.escrowAddress,
    amount: depositData.amountNano.toString(),
  }],
};
await tonConnectUI.sendTransaction(transaction);
```

After sending - polling `dealKeys.detail(dealId)` until the status changes to `FUNDED`.

### Error states

| Error | UI |
|--------|----|
| Wallet rejected | Toast `t('wallet.error.walletRejected')` |
| Not enough TON | Toast `t('wallet.error.insufficientTon')` |
| Timeout | Toast `t('wallet.error.timeout')` |

---

## 3.9 Opening a dispute

| | |
|---|---|
| **Route** | `/deals/:dealId/dispute` (POST form when there is no dispute yet) |
| **Target** | File a transaction dispute |
| **Who sees** | Advertiser or Owner/Manager (`view_deals`) in funded status (`FUNDED`...`DELIVERY_VERIFYING`) |

### API

```
GET  /api/v1/deals/:dealId           # \u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0441\u0442\u0430\u0442\u0443\u0441\u0430
POST /api/v1/deals/:dealId/dispute   # \u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0441\u043f\u043e\u0440
```

### UI

- **Select `t('deals.dispute.reason')`** — enum:
  - `POST_DELETED` — `t('deals.dispute.reason.postDeleted')`
  - `POST_EDITED` — `t('deals.dispute.reason.postEdited')`
  - `WRONG_CONTENT` — `t('deals.dispute.reason.wrongContent')`
  - `QUALITY_ISSUE` — `t('deals.dispute.reason.qualityIssue')`
  - `OTHER` — `t('deals.dispute.reason.other')`
- **Input `t('deals.dispute.description')`** — `textarea`, max 5000 characters
- **Loading evidence** - screenshots (file picker)
- **Warning** — `destructive` text: `t('deals.dispute.warning')`
- Button `t('deals.dispute.submit')` (`primary`, destructive color)

### Actions

| Action | Result |
|----------|-----------|
| "Submit a dispute" | → `DialogModal` confirmations → `POST /api/v1/deals/:id/dispute` → navigate `/deals/:dealId/dispute` |

### Request body

```typescript
{
  reason: 'POST_DELETED' | 'POST_EDITED' | 'WRONG_CONTENT' | 'QUALITY_ISSUE' | 'OTHER';
  description: string;  // max 5000
}
```

### ABAC

Manager: `view_deals` required (minimum - transaction participant).

### Error states

| Error | UI |
|--------|----|
| Error opening a dispute | Toast `t('common.toast.saveFailed')` |
| 409 status does not allow dispute | Toast `t('errors.conflict')` + auto-refetch |

---

## 3.10 Details of the dispute

| | |
|---|---|
| **Route** | `/deals/:dealId/dispute` (GET type when the dispute is already open) |
| **Target** | View dispute status and evidence |
| **Who sees** | Advertiser or Owner/Manager (`view_deals`) in the status `DISPUTED` |

### API

```
GET /api/v1/deals/:dealId/dispute
```

**Query keys:** `disputeKeys.detail(dealId)`

### UI - view definition

Route `/deals/:dealId/dispute` shows:
- **Form 3.9** - if there is no dispute yet (`GET` returned 404 or deal status != `DISPUTED`)
- **Details 3.10** - if the dispute is open (`GET` returned the data)

###UI (details)

- **Dispute status** – badge
- **Reason and description** - from the initiator
- **Group `t('deals.dispute.evidence')`** — timeline (append-only):
  - Each element: author + content (screenshots + text + links) + time
- **Result** - if allowed: decision + justification
- Button `t('deals.dispute.addEvidence')` (`secondary`) - if the dispute is open

### Actions

| Action | Result |
|----------|-----------|
| "Add evidence" | → `/deals/:dealId/dispute/evidence` |

### Error states

| Error | UI |
|--------|----|
| Error loading dispute | `ErrorScreen` + retry |
| 404 dispute not found | `ErrorScreen` `t('errors.notFound.title')` + navigate `/deals/:dealId` |

---

## 3.11 Submission of evidence

| | |
|---|---|
| **Route** | `/deals/:dealId/dispute/evidence` |
| **Target** | Add evidence to an open dispute |
| **Who sees** | Advertiser or Owner/Manager (`view_deals`) in the status `DISPUTED` |

### API

```
GET  /api/v1/deals/:dealId/dispute            # \u041a\u043e\u043d\u0442\u0435\u043a\u0441\u0442 \u0441\u043f\u043e\u0440\u0430
POST /api/v1/deals/:dealId/dispute/evidence   # \u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u0434\u043e\u043a\u0430\u0437\u0430\u0442\u0435\u043b\u044c\u0441\u0442\u0432\u0430
```

### UI - combined form

One feed = combination of all types (at least one field is required):

- **Section `t('deals.evidence.screenshots')`** — file upload, up to 5 screenshots, thumbnails grid
- **Section `t('deals.evidence.description')`** — `textarea`, max 5000 characters
- **Section `t('deals.evidence.links')`** — up to 3 URL inputs, button `t('deals.evidence.addLink')` (`link`)
- **Input `t('deals.evidence.comment')`** — `textarea`, general comment
- Button `t('deals.evidence.submit')` (`primary`) - active if at least one field is filled in

### Request body

```typescript
{
  screenshots?: string[];  // URLs \u043f\u043e\u0441\u043b\u0435 \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438, max 5
  description?: string;    // max 5000
  links?: string[];        // max 3, valid URLs
  comment?: string;        // \u043e\u0431\u0449\u0438\u0439 \u043a\u043e\u043c\u043c\u0435\u043d\u0442\u0430\u0440\u0438\u0439
}
```

### Validation

- At least one of the fields (`screenshots`, `description`, `links`) must be filled in
- Screenshots: max 5, formats: JPEG/PNG/WebP, max 10MB each
- Links: max 3, valid URL format

### Actions

| Action | Result |
|----------|-----------|
| "Submit" | `POST /api/v1/deals/:id/dispute/evidence` → navigate `/deals/:dealId/dispute` |

### Error states

| Error | UI |
|--------|----|
| Error sending evidence | Toast `t('common.toast.saveFailed')` |
| Error loading screenshots | Toast `t('deals.error.mediaUploadFailed')` |

---

## 3.12 Support Sheet

| | |
|---|---|
| **Route** | N/A (Sheet overlay on 3.2) |
| **Target** | Contacting support regarding a transaction |
| **Who sees** | All participants in the transaction |

### API

```
POST /api/v1/support   # \u0421\u043e\u0437\u0434\u0430\u0451\u0442 \u0442\u0438\u043a\u0435\u0442
```

### UI

- Header: `t('deals.support.title')`
- **Select `t('deals.support.topicLabel')`**:
  - `PAYMENT_ISSUE` — `t('deals.support.topic.payment')`
  - `CREATIVE_ISSUE` — `t('deals.support.topic.creative')`
  - `OTHER` — `t('deals.support.topic.other')`
- **Input `t('deals.support.descriptionLabel')`** — `textarea`, max 5000
- **Read-only context** (substituted automatically):
  - Deal ID
  - Current status
  - Transaction amount
  - The role of the addressee
- Button `t('deals.support.submit')` (`primary`)

### Request body

```typescript
{
  dealId: string;
  topic: 'PAYMENT_ISSUE' | 'CREATIVE_ISSUE' | 'OTHER';
  description: string;  // max 5000
  context: {
    dealStatus: DealStatus;
    amountNano: bigint;
    role: DealRole;
  };
}
```

### Actions

| Action | Result |
|----------|-----------|
| "Submit" | `POST /api/v1/support` → toast `t('deals.support.sent')` → close sheet |

The bot forwards the ticket to operators in the support group.

### Error states

| Error | UI |
|--------|----|
| Submit Error | Toast `t('common.toast.saveFailed')` |

---

## File structure

```
src/pages/deals/
  DealsPage.tsx                # Route: /deals
  DealDetailPage.tsx           # Route: /deals/:dealId
  CreateDealPage.tsx           # Route: /deals/new
  NegotiatePage.tsx            # Route: /deals/:dealId/negotiate
  BriefPage.tsx                # Route: /deals/:dealId/brief
  CreativePage.tsx             # Route: /deals/:dealId/creative
  CreativeReviewPage.tsx       # Route: /deals/:dealId/creative/review
  SchedulePage.tsx             # Route: /deals/:dealId/schedule
  DisputePage.tsx              # Route: /deals/:dealId/dispute (form + details)
  DisputeEvidencePage.tsx      # Route: /deals/:dealId/dispute/evidence

src/features/deals/
  api/
    deals.ts
  components/
    DealListItem.tsx
    DealActions.tsx             # \u041c\u0430\u0442\u0440\u0438\u0446\u0430 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0439
    DealTimeline.tsx
    DealStatusBadge.tsx
    PaymentSheet.tsx            # TON Connect sheet
    SupportSheet.tsx            # Support ticket sheet
    TelegramPostPreview.tsx     # \u041f\u0440\u0435\u0432\u044c\u044e \u043a\u0440\u0435\u0430\u0442\u0438\u0432\u0430
    ButtonBuilder.tsx           # Builder \u043a\u043d\u043e\u043f\u043e\u043a \u0434\u043b\u044f \u043a\u0440\u0435\u0430\u0442\u0438\u0432\u0430
    EvidenceForm.tsx            # \u041a\u043e\u043c\u0431\u0438\u043d\u0438\u0440\u043e\u0432\u0430\u043d\u043d\u0430\u044f \u0444\u043e\u0440\u043c\u0430 \u0434\u043e\u043a\u0430\u0437\u0430\u0442\u0435\u043b\u044c\u0441\u0442\u0432
    EvidenceTimeline.tsx
    CreativeImportFlow.tsx      # \u0418\u043c\u043f\u043e\u0440\u0442 \u043f\u043e\u0441\u0442\u0430 \u0447\u0435\u0440\u0435\u0437 \u0431\u043e\u0442\u0430
  hooks/
    useDealRole.ts
    useDealActions.ts
  lib/
    deal-status.ts              # StatusConfig mapping
    deal-actions.ts             # Action matrix
  types/
    deal.ts                     # Zod schemas
    creative.ts
    dispute.ts
    support.ts
```
