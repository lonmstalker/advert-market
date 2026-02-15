package com.advertmarket.marketplace.channel.web;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Query params for {@code GET /api/v1/channels/count}.
 *
 * <p>Matches the filter subset of {@code GET /api/v1/channels}.
 */
public record ChannelCountRequestParams(
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
        @Nullable String language
) {
}

