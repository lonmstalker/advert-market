package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Pricing rule representation.
 *
 * @param id          rule ID
 * @param channelId   channel this rule belongs to
 * @param name        rule display name
 * @param description rule description
 * @param postType    type of post (e.g. "repost", "native", "story")
 * @param priceNano   price in nanoTON
 * @param isActive    whether the rule is active
 * @param sortOrder   display order
 */
@Schema(description = "Channel pricing rule")
public record PricingRuleDto(
        long id,
        long channelId,
        @NonNull String name,
        @Nullable String description,
        @NonNull String postType,
        long priceNano,
        boolean isActive,
        int sortOrder
) {
}
