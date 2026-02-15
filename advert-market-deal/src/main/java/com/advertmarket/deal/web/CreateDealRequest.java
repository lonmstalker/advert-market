package com.advertmarket.deal.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * REST request body for creating a deal.
 */
record CreateDealRequest(
        @NotNull @Positive Long channelId,
        @Positive long amountNano,
        @Nullable Long pricingRuleId,
        @Nullable String creativeBrief) {
}
