package com.advertmarket.marketplace.channel.web;

import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Query params for {@code GET /api/v1/channels}.
 *
 * <p>Includes backward-compatible aliases (e.g. {@code q}, {@code minSubs}).
 */
public record ChannelSearchRequestParams(
        @Nullable String query,
        @Nullable String q,
        @Nullable String category,
        @Nullable Integer minSubscribers,
        @Nullable Integer minSubs,
        @Nullable Integer maxSubscribers,
        @Nullable Integer maxSubs,
        @Nullable Long minPrice,
        @Nullable Long maxPrice,
        @Nullable Double minEngagement,
        @Nullable String language,
        @Nullable String sort,
        @Nullable String cursor,
        @Nullable Integer limit
) {

    /**
     * Canonical constructor.
     *
     * <p>Applies {@link ChannelSearchCriteria#DEFAULT_LIMIT} when {@code limit} is not provided.
     */
    public ChannelSearchRequestParams {
        if (limit == null) {
            limit = ChannelSearchCriteria.DEFAULT_LIMIT;
        }
    }

    /**
     * Returns the requested limit or {@link ChannelSearchCriteria#DEFAULT_LIMIT}
     * when not specified.
     */
    public int limitOrDefault() {
        return limit != null ? limit : ChannelSearchCriteria.DEFAULT_LIMIT;
    }
}
