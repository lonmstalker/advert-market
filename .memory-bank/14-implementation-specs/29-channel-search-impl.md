# Channel Search Implementation

## Overview

Implementation of channel search in the marketplace: jOOQ dynamic query building, index strategy, keyset cursor pagination.

---

## Search API

```
GET /api/v1/channels?topic=crypto&minSubscribers=1000&maxPrice=100000000000&sort=subscribers_desc&cursor=xxx&limit=20
```

### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `topic` | String | -- | Filter by topic (exact match) |
| `minSubscribers` | Integer | -- | Minimum subscriber count |
| `maxSubscribers` | Integer | -- | Maximum subscriber count |
| `minPrice` | Long | -- | Minimum price (nanoTON) |
| `maxPrice` | Long | -- | Maximum price (nanoTON) |
| `minEngagement` | Decimal | -- | Minimum engagement rate |
| `language` | String | -- | Channel content language |
| `available` | Boolean | -- | Only channels accepting deals now |
| `query` | String | -- | Full-text search in title + description |
| `sort` | String | `relevance` | Sort order |
| `cursor` | String | -- | Pagination cursor (opaque) |
| `limit` | Integer | 20 | Page size (max 50) |

### Sort Options

| Value | SQL ORDER BY |
|-------|-------------|
| `relevance` | `ts_rank(tsv, query) DESC, subscriber_count DESC` |
| `subscribers_desc` | `subscriber_count DESC` |
| `subscribers_asc` | `subscriber_count ASC` |
| `price_asc` | `price_per_post_nano ASC` |
| `price_desc` | `price_per_post_nano DESC` |
| `engagement_desc` | `engagement_rate DESC` |
| `updated` | `updated_at DESC` |

---

## jOOQ Dynamic Query Building

```java
public record ChannelSearchCriteria(
    String topic,
    Integer minSubscribers,
    Integer maxSubscribers,
    Long minPrice,
    Long maxPrice,
    BigDecimal minEngagement,
    String language,
    Boolean available,
    String query,
    ChannelSort sort,
    String cursor,
    int limit
) {}

public Page<ChannelListingDto> search(ChannelSearchCriteria criteria) {
    var c = CHANNELS;
    var conditions = new ArrayList<Condition>();

    // Base: only active channels
    conditions.add(c.IS_ACTIVE.isTrue());

    // Dynamic filters
    if (criteria.topic() != null)
        conditions.add(c.TOPIC.eq(criteria.topic()));
    if (criteria.minSubscribers() != null)
        conditions.add(c.SUBSCRIBER_COUNT.ge(criteria.minSubscribers()));
    if (criteria.maxSubscribers() != null)
        conditions.add(c.SUBSCRIBER_COUNT.le(criteria.maxSubscribers()));
    if (criteria.minPrice() != null)
        conditions.add(c.PRICE_PER_POST_NANO.ge(criteria.minPrice()));
    if (criteria.maxPrice() != null)
        conditions.add(c.PRICE_PER_POST_NANO.le(criteria.maxPrice()));
    if (criteria.minEngagement() != null)
        conditions.add(c.ENGAGEMENT_RATE.ge(criteria.minEngagement()));

    // Full-text search
    if (criteria.query() != null && !criteria.query().isBlank()) {
        var tsQuery = toTsQuery(criteria.query());
        conditions.add(DSL.condition("tsv @@ to_tsquery('simple', {0})", tsQuery));
    }

    // Cursor condition
    if (criteria.cursor() != null) {
        conditions.add(decodeCursorCondition(criteria.cursor(), criteria.sort()));
    }

    // Build query
    var query = dsl.select(/* fields */)
        .from(c)
        .where(DSL.and(conditions))
        .orderBy(sortFields(criteria.sort()))
        .limit(criteria.limit() + 1); // fetch n+1 for hasNext

    var results = query.fetch(/* mapper */);

    boolean hasNext = results.size() > criteria.limit();
    if (hasNext) results.removeLast();

    String nextCursor = hasNext ? encodeCursor(results.getLast(), criteria.sort()) : null;
    return new Page<>(results, nextCursor);
}
```

---

## Keyset Cursor Pagination

### Why Keyset (Not Offset)

| Approach | Problem |
|----------|---------|
| OFFSET | Performance degrades linearly: `OFFSET 10000` scans 10000 rows |
| Keyset | Constant performance: uses index seek |

### Cursor Format

Base64-encoded JSON containing sort key values + tie-breaker:

```json
{
  "v": 1,
  "sk": 15000,          // sort key value (e.g., subscriber_count)
  "id": 123456789       // tie-breaker (channel_id)
}
```

Encoded: `eyJ2IjoxLCJzayI6MTUwMDAsImlkIjoxMjM0NTY3ODl9`

### Cursor Condition Generation

```java
private Condition decodeCursorCondition(String cursor, ChannelSort sort) {
    var parsed = decodeCursor(cursor);
    var sortField = sortKeyField(sort);
    var id = CHANNELS.ID;

    // For DESC sort: (sort_key < cursor_sk) OR (sort_key = cursor_sk AND id < cursor_id)
    // For ASC sort:  (sort_key > cursor_sk) OR (sort_key = cursor_sk AND id > cursor_id)
    if (sort.isDescending()) {
        return sortField.lt(parsed.sortKey())
            .or(sortField.eq(parsed.sortKey()).and(id.lt(parsed.id())));
    } else {
        return sortField.gt(parsed.sortKey())
            .or(sortField.eq(parsed.sortKey()).and(id.gt(parsed.id())));
    }
}
```

### Tie-Breaker

`channel_id` (BIGINT PK) - guarantees uniqueness with the same sort key values.

---

## Full-Text Search

### tsvector Column

```sql
ALTER TABLE channels ADD COLUMN tsv tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(description, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(topic, '')), 'A')
    ) STORED;
```

Weight A for title/topic (more relevant), weight B for description.

### Query Parsing

```java
private String toTsQuery(String userInput) {
    // Sanitize: remove special chars, split into words, join with &
    return Arrays.stream(userInput.strip().split("\\s+"))
        .filter(w -> w.length() >= 2)
        .map(w -> w.replaceAll("[^\\p{L}\\p{N}]", ""))
        .filter(w -> !w.isEmpty())
        .collect(Collectors.joining(" & "));
}
```

---

## Index Strategy

### Required Indexes

```sql
-- Primary search index (composite for common filter + sort)
CREATE INDEX idx_channels_active_subscribers
    ON channels (subscriber_count DESC, id DESC)
    WHERE is_active = TRUE;

CREATE INDEX idx_channels_active_price
    ON channels (price_per_post_nano ASC, id ASC)
    WHERE is_active = TRUE;

CREATE INDEX idx_channels_active_engagement
    ON channels (engagement_rate DESC, id DESC)
    WHERE is_active = TRUE;

CREATE INDEX idx_channels_active_updated
    ON channels (updated_at DESC, id DESC)
    WHERE is_active = TRUE;

-- Topic filter (low cardinality -> partial index per popular topic)
CREATE INDEX idx_channels_topic
    ON channels (topic, subscriber_count DESC)
    WHERE is_active = TRUE;

-- Full-text search
CREATE INDEX idx_channels_tsv ON channels USING GIN (tsv);

-- Owner lookup
CREATE INDEX idx_channels_owner ON channels (owner_id);
```

### Index Selection Rationale

| Query Pattern | Index Used |
|-------------|-----------|
| Browse (sort by subscribers) | `idx_channels_active_subscribers` |
| Browse (sort by price) | `idx_channels_active_price` |
| Filter by topic + sort | `idx_channels_topic` (topic prefix), then sort |
| Full-text search | `idx_channels_tsv` (GIN) |
| Channel owner dashboard | `idx_channels_owner` |

---

## Response Schema

```json
{
  "items": [
    {
      "id": 123456789,
      "title": "Crypto News",
      "topic": "crypto",
      "subscriberCount": 15000,
      "avgViews": 5000,
      "engagementRate": 33.33,
      "pricePerPostNano": 50000000000,
      "pricingRules": [
        {"name": "Standard post", "postType": "STANDARD", "priceNano": 50000000000},
        {"name": "Pinned 24h", "postType": "PINNED", "priceNano": 150000000000}
      ],
      "isAvailable": true,
      "updatedAt": "2025-01-15T10:30:00Z"
    }
  ],
  "nextCursor": "eyJ2IjoxLC...",
  "hasNext": true
}
```

---

## Performance Considerations

| Concern | Mitigation |
|---------|-----------|
| Large result sets | Keyset pagination (constant time) |
| Complex filters | Partial indexes on `is_active = TRUE` |
| Full-text overhead | GIN index, `simple` dictionary (no stemming overhead) |
| N+1 pricing rules | JOIN `channel_pricing_rules` in single query |
| Count queries | Avoid total count. Use `hasNext` flag (n+1 pattern) |

### Query Performance Target

| Metric | Target |
|--------|--------|
| p50 latency | < 20ms |
| p95 latency | < 100ms |
| p99 latency | < 300ms |

---

## Related Documents

- [Channel Marketplace Feature](../03-feature-specs/01-channel-marketplace.md)
- [Data Stores](../04-architecture/05-data-stores.md)
- [API Contracts](../11-api-contracts.md)
- [Commission & Pricing](./25-commission-rounding-sweep.md)
