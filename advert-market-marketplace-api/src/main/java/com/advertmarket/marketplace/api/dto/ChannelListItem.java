package com.advertmarket.marketplace.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Lightweight channel representation for search results.
 *
 * @param id               channel (Telegram chat) ID
 * @param title            channel title
 * @param username         public username without @
 * @param category         channel category
 * @param subscriberCount  number of subscribers
 * @param avgViews         average post views
 * @param engagementRate   engagement rate percentage
 * @param pricePerPostNano price per post in nanoTON
 * @param isActive         whether the channel is available
 * @param updatedAt        last update timestamp
 */
public record ChannelListItem(
        long id,
        @NonNull String title,
        @Nullable String username,
        @Nullable String category,
        int subscriberCount,
        int avgViews,
        @Nullable BigDecimal engagementRate,
        @Nullable Long pricePerPostNano,
        boolean isActive,
        @NonNull OffsetDateTime updatedAt
) {
}
