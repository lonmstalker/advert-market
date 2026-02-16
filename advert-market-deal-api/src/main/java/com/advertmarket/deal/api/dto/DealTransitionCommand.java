package com.advertmarket.deal.api.dto;

import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Command to transition a deal to a new status.
 *
 * @param dealId target deal identifier
 * @param targetStatus desired status
 * @param actorId initiating user ID (null for SYSTEM)
 * @param actorType type of actor performing the transition
 * @param reason optional reason (e.g. cancellation reason)
 * @param partialRefundNano partial refund amount in nanoTON
 *                          (required for DISPUTED -> PARTIALLY_REFUNDED)
 * @param partialPayoutNano partial payout amount in nanoTON
 *                          (required for DISPUTED -> PARTIALLY_REFUNDED)
 */
@Schema(description = "Deal state transition command")
public record DealTransitionCommand(
        @NonNull DealId dealId,
        @NonNull DealStatus targetStatus,
        @Nullable Long actorId,
        @NonNull ActorType actorType,
        @Nullable String reason,
        @Nullable Long partialRefundNano,
        @Nullable Long partialPayoutNano) {
}
