package com.advertmarket.marketplace.api.dto;

import com.advertmarket.marketplace.api.model.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Pricing rule representation.
 *
 * @param id          rule ID
 * @param channelId   channel this rule belongs to
 * @param name        rule display name
 * @param description rule description
 * @param postTypes   post types covered by this rule
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
        @NonNull Set<PostType> postTypes,
        long priceNano,
        boolean isActive,
        int sortOrder
) {

    /** Defensively copies post types. */
    public PricingRuleDto {
        postTypes = Set.copyOf(postTypes);
    }
}
