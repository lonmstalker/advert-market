# Channel catalog

> Tab 1. Search and select channels for advertising + create a deal.

## Navigation

```
/catalog
  ├── [Sheet] Filters
  ├── /catalog/channels/:channelId
  └── /deals/new?channelId=:channelId
```

---

## 2.1 Channel list

| | |
|---|---|
| **Route** | `/catalog` |
| **Target** | Search and view channels for advertising |
| **Who sees** | All authorized |

### API

```
GET /api/v1/channels?cursor=&limit=20&query=&category=&minSubscribers=&maxSubscribers=&minPrice=&maxPrice=&sort=
GET /api/v1/channels/count?query=&category=&minSubscribers=&maxSubscribers=&minPrice=&maxPrice=
```

Compatibility aliases supported by backend search:

- `q -> query`
- `minSubs -> minSubscribers`
- `maxSubs -> maxSubscribers`
- `sort=subscribers|price_asc|price_desc|er` (legacy frontend sort values)

**Query keys:** `channelKeys.list(params)`

### Deep link at login

If there is `startapp=channel_{id}` in the Telegram Mini App parameters, automatic routing to `/catalog/channels/:id` (processing in the root router, see 6.4).

### UI

- **Search line** - top, debounce 300ms, placeholder: `t('catalog.search.placeholder')`
- **Button `t('catalog.filters.button')`** - to the right of the search, badge with the number of active filters
- **Channel list** — `Group` + `GroupItem`:
  - `before`: channel avatar (40×40)
  - Title: channel name
  - `subtitle`: `t('catalog.channel.subscribers', { count })`
  - `after`: price (min. from pricing rules, format `<Amount>`)
- **Infinite scroll** — skeleton loading (3 GroupItem placeholder)
- **Pull-to-refresh**

### Actions

| Action | Result |
|----------|-----------|
| Entering search | Debounce → requery with `query=` (or `q=` alias) |
| "Filters" | → Sheet 2.2 |
| Tap on channel | → `/catalog/channels/:channelId` |
| Pull-to-refresh | Invalidate `channelKeys.lists()` |

### Empty state

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `🔍` | `catalog.empty.title` | `catalog.empty.description` | `catalog.empty.cta` → Reset filters |

### Filter status

```typescript
type CatalogFilters = {
  q?: string;             // FE key, sent as query alias
  category?: string;
  minSubs?: number;       // FE key, sent as minSubscribers alias
  maxSubs?: number;       // FE key, sent as maxSubscribers alias
  minPrice?: bigint; // nanoTON
  maxPrice?: bigint; // nanoTON
  sort?: 'relevance' | 'subscribers' | 'price_asc' | 'price_desc' | 'er';
};
```

Storage: URL search params (shareable, back-compatible).

### Error states

| Error | UI |
|--------|----|
| Error loading list | `ErrorScreen` + retry |
| Offline | Banner `t('errors.offline')` |

---

## 2.2 Filters (Sheet)

| | |
|---|---|
| **Route** | N/A (Sheet overlay above the directory) |
| **Target** | Setting channel search parameters |
| **Who sees** | Everyone who opened filters |

### API

```
GET /api/v1/categories # Source for category selector
```

### UI

- Header: `t('catalog.filters.title')`
- **Category** — `Select`, `t('catalog.filters.topic')`
- **Subscribers** - two `Input` (numeric): `t('catalog.filters.from')` / `t('catalog.filters.to')`
- **Price per post** - two `Input` (numeric, TON): `t('catalog.filters.from')` / `t('catalog.filters.to')`
- **Sorting** — `Select`, `t('catalog.filters.sort')`:
  - `t('catalog.filters.sort.relevance')` (default)
  - `t('catalog.filters.sort.subscribers')`
  - `t('catalog.filters.sort.priceAsc')`
  - `t('catalog.filters.sort.priceDesc')`
  - `t('catalog.filters.sort.er')`
- Button `t('catalog.filters.show', { count: N })` (`primary`, full-width) - N is updated when filters are changed
- Button `t('catalog.filters.reset')` (`link`)

### Actions

| Action | Result |
|----------|-----------|
| Changing filters | Local state sheet. Prefetch count for "Show N" |
| "Show" | Apply filters → close sheet → reload catalog |
| "Reset" | Clear all filters |
| Swipe down | Close without application |

### Components

- `Sheet`
- `Select`
- `Input` (numeric)
- `Button` (primary + link)

### Error states

| Error | UI |
|--------|----|
| Error loading themes | Inline error + retry |

---

## 2.3 Channel card

| | |
|---|---|
| **Route** | `/catalog/channels/:channelId` |
| **Target** | Complete channel information before creating a deal |
| **Who sees** | All |

### API

```
GET /api/v1/channels/:channelId
GET /api/v1/channels/:channelId/team # Check: user role (backend returns List<TeamMemberDto>)
```

**Query keys:** `channelKeys.detail(channelId)`, `channelKeys.team(channelId)`

### UI

- **Header row:**
  - **Avatar** – large (80×80)
  - **Name** — `title1`
  - **ShareButton** — share icon, next to the title (see 6.4)
- **Description** — `body`
- **Group `t('catalog.channel.stats')`** — `GroupItem`:
  - `t('catalog.channel.subscribers')` (formatted number)
  - `t('catalog.channel.avgReach')`
  - `t('catalog.channel.er')` (%)
- **Group `t('catalog.channel.pricing')`** — `GroupItem` for each pricing rule:
  - Post type name
  - `after`: price in TON (`<Amount>`)
- **Topics** — caption badges
- **Buttons** (sticky bottom, full-width):
  - If NOT member: `t('catalog.channel.createDeal')` (`primary`) → 2.4
  - If **owner**: BOTH buttons - `t('catalog.channel.edit')` (`secondary`) + `t('catalog.channel.createDeal')` (`primary`)
  - If manager: only `t('catalog.channel.createDeal')` (`primary`)

### ABAC

| Role | Buttons |
|------|--------|
| Outsider | "Create deal" |
| Owner | "Edit" + "Create deal" (can test as an advertiser) |
| Manager (any rights) | "Create deal" |

> "Edit" - **OWNER-ONLY** (`@channelAuth.isOwner` on the backend). Managers DO NOT see this button, even with `manage_listings`.

### Actions

| Action | Result |
|----------|-----------|
| "Create deal" | → `/deals/new?channelId=:channelId` |
| "Edit" | → `/profile/channels/:channelId/edit` (owner only) |
| ShareButton | Deep link `t.me/adv_markt_bot/app?startapp=channel_{channelId}` → `switchInlineQuery()` or clipboard + toast |
| BackButton | → `/catalog` |

### Role Definition

```typescript
const { isOwner, hasRight } = useChannelRights(channelId); // payload normalized in FE (list or {members})
```

### Error states

| Error | UI |
|--------|----|
| 404 channel not found | `ErrorScreen` `t('errors.notFound.title')` + navigate `/catalog` |
| Loading Error | `ErrorScreen` + retry |

---

## 2.4 Creating a deal

| | |
|---|---|
| **Route** | `/deals/new?channelId=:channelId` |
| **Target** | Create an offer for advertising |
| **Who sees** | Anyone authorized (becomes an advertiser) |

### API

```
GET  /api/v1/channels/:channelId    # Pricing rules
POST /api/v1/deals # Create a deal
```

**Mutation:** invalidates `dealKeys.lists()`

### UI

- **Channel card** (read-only, compact): avatar + name + subscribers
- **Select `t('deals.create.postType')`** — from the pricing rules of the channel
- **Price** — `title2`, `tabular-nums`, read-only (updated when selecting type)
- **Input `t('deals.create.message')`** — `textarea`, optional, max 2000 characters, placeholder: `t('deals.create.messagePlaceholder')`
- Button `t('deals.create.submit')` (`primary`, full-width)

### Actions

| Action | Result |
|----------|-----------|
| Selecting a post type | Update displayed price from pricing rule |
| "Send offer" | `POST /api/v1/deals` → navigate `/deals/:newDealId` |

### Request body

```typescript
{
  channelId: number;      // from URL params
  pricingRuleId: number;  // from Select
  message?: string;       // from textarea
}
```

### Validation

- `pricingRuleId` — required (Select cannot be empty)
- `message` — optional, max 2000 characters

### Error states

| Error | UI |
|--------|----|
| 409 (deal already exists) | Toast `t('deals.error.alreadyExists')` |
| Creation Error | Toast `t('common.toast.saveFailed')` |

---

## File structure

```
src/pages/catalog/
  CatalogPage.tsx              # Route: /catalog
  ChannelDetailPage.tsx        # Route: /catalog/channels/:channelId

src/pages/deals/
  CreateDealPage.tsx           # Route: /deals/new

src/features/channels/
  api/
    channels.ts                # API calls
  components/
    ChannelCard.tsx             # Compact channel card (reused in deal pages)
    ChannelFiltersSheet.tsx     # Sheet overlay
    ChannelListItem.tsx # GroupItem for the list
    ChannelStats.tsx # Statistics group
    PricingRulesList.tsx # List of prices
  hooks/
    useChannelFilters.ts        # URL search params state
    useChannelRights.ts         # ABAC hook (isOwner, hasRight)
  types/
    channel.ts                  # Zod schemas + types
```
