package com.advertmarket.marketplace.channel.search;

import static com.advertmarket.db.generated.tables.Categories.CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;

import com.advertmarket.marketplace.api.dto.ChannelListItem;
import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import com.advertmarket.marketplace.api.dto.ChannelSort;
import com.advertmarket.marketplace.api.port.CategoryRepository;
import com.advertmarket.marketplace.api.port.ChannelSearchPort;
import com.advertmarket.marketplace.channel.mapper.ChannelListItemMapper;
import com.advertmarket.marketplace.channel.mapper.ChannelRow;
import com.advertmarket.shared.pagination.CursorCodec;
import com.advertmarket.shared.pagination.CursorPage;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Channel search implementation using ParadeDB pg_search BM25 index.
 *
 * <p>Uses fuzzy matching with distance=1 for the text query,
 * standard jOOQ conditions for filters, and keyset cursor pagination.
 */
@Component
@RequiredArgsConstructor
public class ParadeDbChannelSearch implements ChannelSearchPort {

    private final DSLContext dsl;
    private final CategoryRepository categoryRepository;
    private final ChannelListItemMapper channelListItemMapper;

    // ParadeDB uses pdb.score(id) for BM25 relevance. Keep it unqualified
    // (pdb.score(id), not pdb.score(channels.id)) to match pg_search expectations.
    private static final org.jooq.Field<BigDecimal> SCORE_FIELD =
            DSL.field("pdb.score(id)::numeric", BigDecimal.class);

    @Override
    @NonNull
    public CursorPage<ChannelListItem> search(
            @NonNull ChannelSearchCriteria criteria) {
        Condition condition = buildSearchCondition(criteria);
        boolean hasTextQuery = criteria.query() != null
                && !criteria.query().isBlank();

        if (hasTextQuery && criteria.sort() == ChannelSort.RELEVANCE) {
            return searchByRelevance(criteria, condition);
        }

        condition = applyKeyset(condition, criteria, hasTextQuery);

        List<OrderField<?>> orderBy = buildOrderBy(criteria.sort(),
                hasTextQuery);

        // n+1 pattern: fetch one extra to determine hasNext
        int fetchLimit = criteria.limit() + 1;

        var rows = dsl.select(
                        CHANNELS.ID.as("id"),
                        CHANNELS.TITLE.as("title"),
                        CHANNELS.USERNAME.as("username"),
                        CHANNELS.SUBSCRIBER_COUNT.as("subscriberCount"),
                        CHANNELS.AVG_VIEWS.as("avgViews"),
                        CHANNELS.ENGAGEMENT_RATE.as("engagementRate"),
                        CHANNELS.PRICE_PER_POST_NANO.as("pricePerPostNano"),
                        CHANNELS.IS_ACTIVE.as("isActive"),
                        CHANNELS.UPDATED_AT.as("updatedAt"))
                .from(CHANNELS)
                .where(condition)
                .orderBy(orderBy)
                .limit(fetchLimit)
                .fetchInto(ChannelRow.class);

        boolean hasNext = rows.size() > criteria.limit();
        var pageRows = hasNext
                ? rows.subList(0, criteria.limit())
                : rows;

        List<Long> channelIds = pageRows.stream()
                .map(r -> r.id())
                .toList();
        Map<Long, List<String>> categoriesByChannel =
                categoryRepository.findCategorySlugsForChannels(channelIds);

        List<ChannelListItem> items = pageRows.stream()
                .map(r -> channelListItemMapper.toDto(
                        r,
                        categoriesByChannel.getOrDefault(r.id(), List.of())))
                .toList();

        String nextCursor = null;
        if (hasNext && !pageRows.isEmpty()) {
            var last = pageRows.getLast();
            nextCursor = buildCursor(last, criteria.sort());
        }

        return new CursorPage<>(items, nextCursor);
    }

    private CursorPage<ChannelListItem> searchByRelevance(
            @NonNull ChannelSearchCriteria criteria,
            @NonNull Condition baseCondition) {
        int fetchLimit = criteria.limit() + 1;

        // ParadeDB currently doesn't support keyset pagination using pdb.score(..) in WHERE.
        // Pragmatic fallback: OFFSET-based pagination for RELEVANCE+query only.
        int offset = 0;
        if (criteria.cursor() != null && !criteria.cursor().isBlank()) {
            Map<String, String> cursor = CursorCodec.decode(criteria.cursor());
            String offsetValue = cursor.get("o");
            if (offsetValue != null) {
                try {
                    offset = Integer.parseInt(offsetValue);
                } catch (NumberFormatException ignore) {
                    offset = 0;
                }
            }
        }

        var rows = dsl.select(
                        CHANNELS.ID.as("id"),
                        CHANNELS.TITLE.as("title"),
                        CHANNELS.USERNAME.as("username"),
                        CHANNELS.SUBSCRIBER_COUNT.as("subscriberCount"),
                        CHANNELS.AVG_VIEWS.as("avgViews"),
                        CHANNELS.ENGAGEMENT_RATE.as("engagementRate"),
                        CHANNELS.PRICE_PER_POST_NANO.as("pricePerPostNano"),
                        CHANNELS.IS_ACTIVE.as("isActive"),
                        CHANNELS.UPDATED_AT.as("updatedAt"))
                .from(CHANNELS)
                .where(baseCondition)
                .orderBy(SCORE_FIELD.desc(), CHANNELS.ID.desc())
                .limit(fetchLimit)
                .offset(offset)
                .fetchInto(ChannelRow.class);

        boolean hasNext = rows.size() > criteria.limit();
        var pageRows = hasNext
                ? rows.subList(0, criteria.limit())
                : rows;

        List<Long> channelIds = pageRows.stream()
                .map(r -> r.id())
                .toList();
        Map<Long, List<String>> categoriesByChannel =
                categoryRepository.findCategorySlugsForChannels(channelIds);

        List<ChannelListItem> items = pageRows.stream()
                .map(r -> channelListItemMapper.toDto(
                        r,
                        categoriesByChannel.getOrDefault(r.id(), List.of())))
                .toList();

        String nextCursor = null;
        if (hasNext) {
            nextCursor = CursorCodec.encode(
                    Map.of("o", String.valueOf(offset + criteria.limit())));
        }

        return new CursorPage<>(items, nextCursor);
    }

    @Override
    public long count(@NonNull ChannelSearchCriteria criteria) {
        Condition condition = buildSearchCondition(criteria);
        Long value = dsl.selectCount()
                .from(CHANNELS)
                .where(condition)
                .fetchOne(0, Long.class);
        return value == null ? 0L : value;
    }

    private static Condition buildSearchCondition(
            ChannelSearchCriteria criteria) {
        Condition condition = CHANNELS.IS_ACTIVE.isTrue();
        condition = applyFilters(condition, criteria);

        boolean hasTextQuery = criteria.query() != null
                && !criteria.query().isBlank();
        if (hasTextQuery) {
            condition = condition.and(
                    DSL.condition("channels @@@ {0}",
                            DSL.val(buildParadeDbQuery(criteria.query()))));
        }
        return condition;
    }

    private static Condition applyFilters(Condition condition,
                                          ChannelSearchCriteria c) {
        if (c.category() != null) {
            condition = condition.and(
                    DSL.exists(DSL.selectOne()
                            .from(CHANNEL_CATEGORIES)
                            .join(CATEGORIES)
                            .on(CHANNEL_CATEGORIES.CATEGORY_ID
                                    .eq(CATEGORIES.ID))
                            .where(CHANNEL_CATEGORIES.CHANNEL_ID
                                    .eq(CHANNELS.ID))
                            .and(CATEGORIES.SLUG.eq(c.category()))));
        }
        if (c.minSubscribers() != null) {
            condition = condition.and(
                    CHANNELS.SUBSCRIBER_COUNT.ge(c.minSubscribers()));
        }
        if (c.maxSubscribers() != null) {
            condition = condition.and(
                    CHANNELS.SUBSCRIBER_COUNT.le(c.maxSubscribers()));
        }
        if (c.minPrice() != null) {
            condition = condition.and(
                    CHANNELS.PRICE_PER_POST_NANO.ge(c.minPrice()));
        }
        if (c.maxPrice() != null) {
            condition = condition.and(
                    CHANNELS.PRICE_PER_POST_NANO.le(c.maxPrice()));
        }
        if (c.minEngagement() != null) {
            condition = condition.and(
                    CHANNELS.ENGAGEMENT_RATE.ge(
                            BigDecimal.valueOf(c.minEngagement())));
        }
        if (c.language() != null) {
            condition = condition.and(CHANNELS.LANGUAGE.eq(c.language()));
        }
        return condition;
    }

    private static String buildParadeDbQuery(String query) {
        String escaped = query
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace(":", "\\:")
                .replace("~", "\\~")
                .replace("'", "''");
        return "title:\"" + escaped + "\"~1 OR description:\""
                + escaped + "\"~1";
    }

    private static Condition applyKeyset(Condition condition,
                                         ChannelSearchCriteria criteria,
                                         boolean hasTextQuery) {
        if (criteria.cursor() == null || criteria.cursor().isBlank()) {
            return condition;
        }

        Map<String, String> cursor = CursorCodec.decode(criteria.cursor());
        String lastId = cursor.get("id");
        String lastSort = cursor.get("sort");

        if (lastId == null) {
            return condition;
        }

        long lastChannelId = Long.parseLong(lastId);
        ChannelSort sort = criteria.sort();

        if (sort == ChannelSort.RELEVANCE) {
            if (hasTextQuery && lastSort != null) {
                BigDecimal lastScore = new BigDecimal(lastSort);
                condition = condition.and(
                        SCORE_FIELD.lt(lastScore)
                                .or(SCORE_FIELD.eq(lastScore)
                                        .and(CHANNELS.ID.lt(lastChannelId))));
            } else if (!hasTextQuery && lastSort != null) {
                condition = condition.and(
                        buildKeysetCondition(ChannelSort.SUBSCRIBERS_DESC,
                                lastSort, lastChannelId));
            } else {
                // Invalid cursor. Best effort: fall back to id-only.
                condition = condition.and(CHANNELS.ID.lt(lastChannelId));
            }
        } else {
            condition = condition.and(
                    buildKeysetCondition(sort, lastSort, lastChannelId));
        }

        return condition;
    }

    @SuppressWarnings("unchecked")
    private static Condition buildKeysetCondition(ChannelSort sort,
                                                  String lastSortValue,
                                                  long lastId) {
        return switch (sort) {
            case SUBSCRIBERS_DESC -> {
                int val = Integer.parseInt(lastSortValue);
                yield CHANNELS.SUBSCRIBER_COUNT.lt(val)
                        .or(CHANNELS.SUBSCRIBER_COUNT.eq(val)
                                .and(CHANNELS.ID.lt(lastId)));
            }
            case SUBSCRIBERS_ASC -> {
                int val = Integer.parseInt(lastSortValue);
                yield CHANNELS.SUBSCRIBER_COUNT.gt(val)
                        .or(CHANNELS.SUBSCRIBER_COUNT.eq(val)
                                .and(CHANNELS.ID.gt(lastId)));
            }
            case PRICE_ASC -> {
                long val = Long.parseLong(lastSortValue);
                yield CHANNELS.PRICE_PER_POST_NANO.gt(val)
                        .or(CHANNELS.PRICE_PER_POST_NANO.eq(val)
                                .and(CHANNELS.ID.gt(lastId)));
            }
            case PRICE_DESC -> {
                long val = Long.parseLong(lastSortValue);
                yield CHANNELS.PRICE_PER_POST_NANO.lt(val)
                        .or(CHANNELS.PRICE_PER_POST_NANO.eq(val)
                                .and(CHANNELS.ID.lt(lastId)));
            }
            case ENGAGEMENT_DESC -> {
                BigDecimal val = new BigDecimal(lastSortValue);
                yield CHANNELS.ENGAGEMENT_RATE.lt(val)
                        .or(CHANNELS.ENGAGEMENT_RATE.eq(val)
                                .and(CHANNELS.ID.lt(lastId)));
            }
            case UPDATED -> {
                OffsetDateTime val = OffsetDateTime.parse(lastSortValue);
                yield CHANNELS.UPDATED_AT.lt(val)
                        .or(CHANNELS.UPDATED_AT.eq(val)
                                .and(CHANNELS.ID.lt(lastId)));
            }
            case RELEVANCE -> DSL.noCondition();
        };
    }

    private static List<OrderField<?>> buildOrderBy(ChannelSort sort,
                                                    boolean hasTextQuery) {
        List<OrderField<?>> fields = new ArrayList<>();

        if (hasTextQuery && sort == ChannelSort.RELEVANCE) {
            fields.add(SCORE_FIELD.desc());
            fields.add(CHANNELS.ID.desc());
            return fields;
        }

        switch (sort) {
            case SUBSCRIBERS_DESC -> {
                fields.add(CHANNELS.SUBSCRIBER_COUNT.desc());
                fields.add(CHANNELS.ID.desc());
            }
            case SUBSCRIBERS_ASC -> {
                fields.add(CHANNELS.SUBSCRIBER_COUNT.asc());
                fields.add(CHANNELS.ID.asc());
            }
            case PRICE_ASC -> {
                fields.add(CHANNELS.PRICE_PER_POST_NANO.asc());
                fields.add(CHANNELS.ID.asc());
            }
            case PRICE_DESC -> {
                fields.add(CHANNELS.PRICE_PER_POST_NANO.desc());
                fields.add(CHANNELS.ID.desc());
            }
            case ENGAGEMENT_DESC -> {
                fields.add(CHANNELS.ENGAGEMENT_RATE.desc());
                fields.add(CHANNELS.ID.desc());
            }
            case UPDATED -> {
                fields.add(CHANNELS.UPDATED_AT.desc());
                fields.add(CHANNELS.ID.desc());
            }
            case RELEVANCE -> {
                fields.add(CHANNELS.SUBSCRIBER_COUNT.desc());
                fields.add(CHANNELS.ID.desc());
            }
            default -> {}
        }
        return fields;
    }

    private static String buildCursor(ChannelRow last, ChannelSort sort) {
        String id = String.valueOf(last.id());
        String sortValue = switch (sort) {
            case SUBSCRIBERS_DESC, SUBSCRIBERS_ASC ->
                    String.valueOf(last.subscriberCount());
            case PRICE_ASC, PRICE_DESC ->
                    String.valueOf(last.pricePerPostNano());
            case ENGAGEMENT_DESC ->
                    String.valueOf(last.engagementRate());
            case UPDATED ->
                    last.updatedAt().toString();
            // RELEVANCE without query falls back to subscribers desc.
            case RELEVANCE -> String.valueOf(last.subscriberCount());
        };
        return CursorCodec.encode(Map.of("id", id, "sort", sortValue));
    }
}
