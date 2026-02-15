package com.advertmarket.marketplace.channel.mapper;

import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;

/**
 * Normalizes {@link ChannelSearchCriteria} values (e.g. clamping limits).
 *
 * <p>Kept outside {@code ..service..} to avoid DTO construction in services.
 */
public final class ChannelSearchCriteriaNormalizer {

    private ChannelSearchCriteriaNormalizer() {
    }

    /**
     * Normalizes criteria values and clamps pagination limits.
     */
    public static ChannelSearchCriteria normalize(
            ChannelSearchCriteria criteria) {
        int limit = Math.clamp(criteria.limit(), 1,
                ChannelSearchCriteria.MAX_LIMIT);
        if (limit == criteria.limit()) {
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
                criteria.query(),
                criteria.sort(),
                criteria.cursor(),
                limit);
    }
}
