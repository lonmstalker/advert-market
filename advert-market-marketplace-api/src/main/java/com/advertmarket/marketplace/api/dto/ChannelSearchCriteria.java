package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Search criteria for channel catalog.
 *
 * @param category      filter by category
 * @param minSubscribers minimum subscriber count
 * @param maxSubscribers maximum subscriber count
 * @param minPrice      minimum price per post in nanoTON
 * @param maxPrice      maximum price per post in nanoTON
 * @param minEngagement minimum engagement rate
 * @param language      filter by language code
 * @param query         free-text search query (BM25)
 * @param sort          sort order
 * @param cursor        keyset pagination cursor
 * @param limit         page size (1..50)
 */
@Schema(description = "Channel search criteria")
public record ChannelSearchCriteria(
        @Nullable String category,
        @Nullable Integer minSubscribers,
        @Nullable Integer maxSubscribers,
        @Nullable Long minPrice,
        @Nullable Long maxPrice,
        @Nullable Double minEngagement,
        @Nullable String language,
        @Nullable String query,
        @NonNull ChannelSort sort,
        @Nullable String cursor,
        int limit
) {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 50;
}
