package com.advertmarket.marketplace.channel.repository;

import static com.advertmarket.db.generated.tables.Categories.CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;

import com.advertmarket.marketplace.api.dto.CategoryDto;
import com.advertmarket.marketplace.api.port.CategoryRepository;
import com.advertmarket.shared.json.JsonFacade;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.springframework.stereotype.Repository;

/**
 * Implements {@link CategoryRepository} using jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqCategoryRepository implements CategoryRepository {

    private final DSLContext dsl;
    private final JsonFacade jsonFacade;

    @Override
    @NonNull
    public List<CategoryDto> findAllActive() {
        return dsl.selectFrom(CATEGORIES)
                .where(CATEGORIES.IS_ACTIVE.isTrue())
                .orderBy(CATEGORIES.SORT_ORDER.asc())
                .fetch(r -> new CategoryDto(
                        r.getId(),
                        r.getSlug(),
                        parseLocalizedName(r.getLocalizedName()),
                        r.getSortOrder()));
    }

    @Override
    @NonNull
    public List<String> findCategorySlugsForChannel(long channelId) {
        return dsl.select(CATEGORIES.SLUG)
                .from(CHANNEL_CATEGORIES)
                .join(CATEGORIES)
                .on(CHANNEL_CATEGORIES.CATEGORY_ID.eq(CATEGORIES.ID))
                .where(CHANNEL_CATEGORIES.CHANNEL_ID.eq(channelId))
                .orderBy(CATEGORIES.SORT_ORDER.asc())
                .fetch(CATEGORIES.SLUG);
    }

    @Override
    @NonNull
    public Map<Long, List<String>> findCategorySlugsForChannels(
            @NonNull List<Long> channelIds) {
        if (channelIds.isEmpty()) {
            return Map.of();
        }
        return dsl.select(
                        CHANNEL_CATEGORIES.CHANNEL_ID,
                        CATEGORIES.SLUG)
                .from(CHANNEL_CATEGORIES)
                .join(CATEGORIES)
                .on(CHANNEL_CATEGORIES.CATEGORY_ID.eq(CATEGORIES.ID))
                .where(CHANNEL_CATEGORIES.CHANNEL_ID.in(channelIds))
                .orderBy(CHANNEL_CATEGORIES.CHANNEL_ID.asc(),
                        CATEGORIES.SORT_ORDER.asc())
                .fetchGroups(CHANNEL_CATEGORIES.CHANNEL_ID, CATEGORIES.SLUG);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseLocalizedName(JSON json) {
        if (json == null || json.data() == null) {
            return Map.of();
        }
        return jsonFacade.fromJson(json.data(), Map.class);
    }
}
