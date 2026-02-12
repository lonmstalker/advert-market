package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Request to create a pricing rule.
 *
 * @param name        rule display name
 * @param description rule description
 * @param postType    type of post
 * @param priceNano   price in nanoTON
 * @param sortOrder   display order
 */
@Schema(description = "Pricing rule creation request")
public record PricingRuleCreateRequest(
        @NotBlank @Size(max = 100) @NonNull String name,
        @Nullable String description,
        @NotBlank @Size(max = 50) @NonNull String postType,
        @Positive long priceNano,
        int sortOrder
) {
}
