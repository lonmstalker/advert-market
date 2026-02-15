package com.advertmarket.marketplace.channel.mapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * JOOQ projection for channel list/search rows.
 *
 * <p>Used to avoid manual DTO construction in {@code ..search..}.
 */
public record ChannelRow(
        long id,
        @NonNull String title,
        @Nullable String username,
        int subscriberCount,
        int avgViews,
        @Nullable BigDecimal engagementRate,
        @Nullable Long pricePerPostNano,
        boolean isActive,
        @NonNull OffsetDateTime updatedAt
) {
}
