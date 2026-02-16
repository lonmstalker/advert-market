package com.advertmarket.deal.web;

import com.advertmarket.shared.model.DealStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * REST request body for transitioning a deal status.
 */
public record DealTransitionRequest(
        @NotNull DealStatus targetStatus,
        @Nullable String reason,
        @Nullable @Positive Long partialRefundNano,
        @Nullable @Positive Long partialPayoutNano) {
}
