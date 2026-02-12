package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Request to update a pricing rule.
 *
 * @param name        new display name
 * @param description new description
 * @param priceNano   new price in nanoTON
 * @param sortOrder   new display order
 * @param isActive    new active status
 */
@Schema(description = "Pricing rule update request")
public record PricingRuleUpdateRequest(
        @Nullable @Size(max = 100) String name,
        @Nullable String description,
        @Nullable @Positive Long priceNano,
        @Nullable Integer sortOrder,
        @Nullable Boolean isActive
) {
}
