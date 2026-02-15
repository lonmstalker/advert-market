package com.advertmarket.deal.api.dto;

import com.advertmarket.shared.model.DealStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Sealed result of a deal state transition.
 */
public sealed interface DealTransitionResult {

    /** Transition succeeded, deal is now in {@code newStatus}. */
    @Schema(description = "Successful deal state transition")
    record Success(@NonNull DealStatus newStatus) implements DealTransitionResult {
    }

    /** Deal was already in the requested target state. */
    @Schema(description = "Deal was already in the target state")
    record AlreadyInTargetState(@NonNull DealStatus currentStatus) implements DealTransitionResult {
    }
}
