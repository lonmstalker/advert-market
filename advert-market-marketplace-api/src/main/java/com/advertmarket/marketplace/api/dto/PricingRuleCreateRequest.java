package com.advertmarket.marketplace.api.dto;

import com.advertmarket.marketplace.api.model.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Request to create a pricing rule.
 *
 * @param name        rule display name
 * @param description rule description
 * @param postTypes   post types covered by this rule
 * @param priceNano   price in nanoTON
 * @param sortOrder   display order
 */
@Schema(description = "Pricing rule creation request")
public record PricingRuleCreateRequest(
        @NotBlank @Size(max = 100) @NonNull String name,
        @Nullable String description,
        @NotEmpty @NonNull Set<PostType> postTypes,
        @Positive long priceNano,
        int sortOrder
) {

    /** Creates a request with a defensive copy of postTypes. */
    public PricingRuleCreateRequest {
        postTypes = Set.copyOf(postTypes);
    }
}
