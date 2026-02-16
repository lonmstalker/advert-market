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
 * @param categories       category slugs
 * @param pricePerPostNano base price per post in nanoTON
 * @param isActive         whether the channel is available
 * @param ownerId          channel owner user ID
 * @param engagementRate   engagement rate percentage
 * @param avgViews         average post views
 * @param language         channel language code
 * @param rules            channel listing rules (owner note)
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
        @NonNull List<String> categories,
        @Nullable Long pricePerPostNano,
        boolean isActive,
        long ownerId,
        @Nullable BigDecimal engagementRate,
        int avgViews,
        @Nullable String language,
        @Nullable ChannelRules rules,
        @NonNull List<PricingRuleDto> pricingRules,
        @NonNull OffsetDateTime createdAt,
        @NonNull OffsetDateTime updatedAt
) {

    /** Defensively copies categories and pricing rules. */
    public ChannelDetailResponse {
        categories = List.copyOf(categories);
        pricingRules = List.copyOf(pricingRules);
    }

    /**
     * Channel listing rules exposed for frontend compatibility.
     *
     * @param customRules owner note / free-form rule text
     */
    @Schema(description = "Channel listing rules")
    public record ChannelRules(
            @Nullable String customRules
    ) {
    }
}
