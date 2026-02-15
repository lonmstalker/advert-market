package com.advertmarket.deal.web;

import com.advertmarket.shared.model.DealStatus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * REST response for a deal transition result.
 */
public record DealTransitionResponse(
        @NonNull String status,
        @Nullable DealStatus newStatus,
        @Nullable DealStatus currentStatus) {
}
