package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Full channel detail including pricing rules.
 *
 * @param id               channel (Telegram chat) ID
 * @param title            channel title
 * @param username         public username without @
 * @param description      channel description
 * @param subscriberCount  number of subscribers
 * @param category         channel category
 * @param pricePerPostNano base price per post in nanoTON
 * @param isActive         whether the channel is available
 * @param ownerId          channel owner user ID
 * @param engagementRate   engagement rate percentage
 * @param avgViews         average post views
 * @param language         channel language code
 * @param pricingRules     list of pricing rules
 * @param createdAt        creation timestamp
 * @param updatedAt        last update timestamp
 */
@Schema(description = "Full channel detail with pricing rules")
public record ChannelDetailResponse(
        long id,
        @NonNull String title,
        @Nullable String username,
        @Nullable String description,
        int subscriberCount,
        @Nullable String category,
        @Nullable Long pricePerPostNano,
        boolean isActive,
        long ownerId,
        @Nullable BigDecimal engagementRate,
        int avgViews,
        @Nullable String language,
        @NonNull List<PricingRuleDto> pricingRules,
        @NonNull OffsetDateTime createdAt,
        @NonNull OffsetDateTime updatedAt
) {

    public ChannelDetailResponse {
        pricingRules = List.copyOf(pricingRules);
    }
}
