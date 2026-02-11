# Feature: Channel Marketplace

## Overview

The channel marketplace is the entry point for advertisers. It provides a searchable catalog of Telegram channels available for advertising, with verified statistics, pricing, and availability.

## User Stories

- **As an Advertiser**, I want to browse channels by topic, audience size, and price range so I can find the best fit for my campaign.
- **As a Channel Owner**, I want to create and manage my channel listing so advertisers can discover my channel.
- **As a Channel Admin**, I want to update listing details (if rights granted) to keep information current.

## Channel Listing Data

| Field | Type | Description |
|-------|------|-------------|
| `channel_id` | `BIGINT` | Telegram channel ID |
| `owner_id` | `BIGINT` | Telegram user ID of the owner |
| `title` | `VARCHAR` | Channel display name |
| `description` | `TEXT` | Channel description and ad policy |
| `topic` | `VARCHAR` | Channel category/niche |
| `subscriber_count` | `INTEGER` | Current subscriber count |
| `avg_views` | `INTEGER` | Average post views (last 30 days) |
| `engagement_rate` | `DECIMAL` | Engagement percentage |
| `price_per_post_nano` | `BIGINT` | Base price per ad post in nanoTON |
| `availability` | `JSONB` | Available time slots and formats |
| `is_active` | `BOOLEAN` | Whether listing is visible |
| `created_at` | `TIMESTAMPTZ` | Listing creation date |
| `updated_at` | `TIMESTAMPTZ` | Last update timestamp |

## Search & Filters

### Filter Criteria

- **Topic/Category** — predefined list (crypto, tech, lifestyle, news, etc.)
- **Subscriber count range** — min/max subscribers
- **Price range** — min/max nanoTON per post
- **Engagement rate** — minimum threshold
- **Availability** — channels accepting deals now
- **Language** — channel content language

### Sorting Options

- Relevance (default)
- Subscriber count (desc)
- Price (asc/desc)
- Engagement rate (desc)
- Recently updated

## Statistics Verification

Channel statistics are sourced from Telegram Bot API and refreshed with configurable freshness guarantees:

- Subscriber count — via `getChatMemberCount` (lazy refresh on view + daily batch)
- Verified by comparing advertised vs actual stats
- Stale listings (>1h for listing page, >15min for deal creation) trigger async refresh
- Source indicator: `BOT_VERIFIED`, `OWNER_REPORTED`, or `UNVERIFIED`

See [Channel Statistics Verification](./09-channel-statistics.md) for full spec.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/channels` | List/search channels with filters |
| `GET` | `/api/v1/channels/{id}` | Get channel details |
| `POST` | `/api/v1/channels` | Register channel listing (owner) |
| `PUT` | `/api/v1/channels/{id}` | Update listing (owner/admin) |
| `DELETE` | `/api/v1/channels/{id}` | Deactivate listing (owner) |

## Components Involved

| Component | Role |
|-----------|------|
| **Mini App — Deal Flow UI** | Channel browsing and selection interface |
| **Backend API — Channel Service** | Channel CRUD, statistics update, listing management, pricing rules |
| **Backend API — Search Service** | Full-text search + composite filters (topic, subscribers, price), cursor pagination |
| **Backend API — Auth Service** | Verify channel ownership via ABAC (channel_memberships) |
| **PostgreSQL** | Channel listings storage (GIN index for full-text search) |

## Related Documents

- [Deal Lifecycle](./02-deal-lifecycle.md) — what happens after selecting a channel
- [Channel Statistics Verification](./09-channel-statistics.md) — stats collection and freshness
- [Team Management](./07-team-management.md) — who can manage listings
- [Actors and Personas](../02-actors-and-personas.md) — role capabilities
