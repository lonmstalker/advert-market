package com.advertmarket.marketplace.channel.mapper;

import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import java.util.Objects;

/**
 * Normalizes {@link ChannelSearchCriteria} values (e.g. clamping limits).
 *
 * <p>Kept outside {@code ..service..} to avoid DTO construction in services.
 */
public final class ChannelSearchCriteriaNormalizer {

    private ChannelSearchCriteriaNormalizer() {
    }

    private static final int MAX_QUERY_LENGTH = 200;

    /**
     * Normalizes criteria values and clamps pagination limits.
     */
    public static ChannelSearchCriteria normalize(
            ChannelSearchCriteria criteria) {
        int limit = Math.clamp(criteria.limit(), 1,
                ChannelSearchCriteria.MAX_LIMIT);
        String query = criteria.query();
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            query = query.substring(0, MAX_QUERY_LENGTH);
        }
        if (limit == criteria.limit()
                && Objects.equals(query, criteria.query())) {
            return criteria;
        }
        return new ChannelSearchCriteria(
                criteria.category(),
                criteria.minSubscribers(),
                criteria.maxSubscribers(),
                criteria.minPrice(),
                criteria.maxPrice(),
                criteria.minEngagement(),
                criteria.language(),
                query,
                criteria.sort(),
                criteria.cursor(),
                limit);
    }
}
