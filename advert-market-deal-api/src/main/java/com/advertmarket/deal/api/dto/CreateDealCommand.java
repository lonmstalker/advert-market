package com.advertmarket.deal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Command to create a new deal in DRAFT status.
 *
 * @param channelId target channel ID
 * @param amountNano deal amount in nanoTON (must be positive)
 * @param pricingRuleId optional pricing rule reference
 * @param creativeBrief optional creative brief as JSON string
 * @param creativeId optional creative template ID from personal library
 */
@Schema(description = "Command to create a new deal")
public record CreateDealCommand(
        long channelId,
        @Schema(description = "Deal amount in nanoTON, must be positive")
        long amountNano,
        @Nullable Long pricingRuleId,
        @Schema(description = "Creative brief as JSON string")
        @Nullable String creativeBrief,
        @Schema(description = "Creative template id")
        @Nullable String creativeId) {
}
