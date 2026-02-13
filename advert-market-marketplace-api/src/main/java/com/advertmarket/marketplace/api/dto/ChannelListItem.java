package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Lightweight channel representation for search results.
 *
 * @param id               channel (Telegram chat) ID
 * @param title            channel title
 * @param username         public username without @
 * @param categories       category slugs
 * @param subscriberCount  number of subscribers
 * @param avgViews         average post views
 * @param engagementRate   engagement rate percentage
 * @param pricePerPostNano price per post in nanoTON
 * @param isActive         whether the channel is available
 * @param updatedAt        last update timestamp
 */
@Schema(description = "Channel search result item")
public record ChannelListItem(
        long id,
        @NonNull String title,
        @Nullable String username,
        @NonNull List<String> categories,
        int subscriberCount,
        int avgViews,
        @Nullable BigDecimal engagementRate,
        @Nullable Long pricePerPostNano,
        boolean isActive,
        @NonNull OffsetDateTime updatedAt
) {

    public ChannelListItem {
        categories = List.copyOf(categories);
    }
}
