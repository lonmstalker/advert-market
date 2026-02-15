package com.advertmarket.deal.api.dto;

import com.advertmarket.shared.model.DealStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Internal persistence projection matching the {@code deals} table.
 *
 * @param id deal UUID
 * @param channelId associated channel
 * @param advertiserId advertiser user ID
 * @param ownerId channel owner user ID
 * @param pricingRuleId optional pricing rule reference
 * @param status current deal status
 * @param amountNano deal amount in nanoTON
 * @param commissionRateBp commission rate in basis points
 * @param commissionNano commission amount in nanoTON
 * @param depositAddress TON deposit address (nullable)
 * @param subwalletId subwallet identifier (nullable)
 * @param creativeBrief creative brief JSON (nullable)
 * @param creativeDraft creative draft JSON (nullable)
 * @param messageId Telegram message ID (nullable)
 * @param contentHash published content hash (nullable)
 * @param deadlineAt current deadline (nullable)
 * @param publishedAt publication timestamp (nullable)
 * @param completedAt completion timestamp (nullable)
 * @param fundedAt funding timestamp (nullable)
 * @param cancellationReason reason for cancellation (nullable)
 * @param depositTxHash deposit transaction hash (nullable)
 * @param payoutTxHash payout transaction hash (nullable)
 * @param refundedTxHash refund transaction hash (nullable)
 * @param version optimistic lock version
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 */
@Schema(description = "Deal persistence record")
public record DealRecord(
        @NonNull UUID id,
        long channelId,
        long advertiserId,
        long ownerId,
        @Nullable Long pricingRuleId,
        @NonNull DealStatus status,
        long amountNano,
        int commissionRateBp,
        long commissionNano,
        @Nullable String depositAddress,
        @Nullable Integer subwalletId,
        @Nullable String creativeBrief,
        @Nullable String creativeDraft,
        @Nullable Long messageId,
        @Nullable String contentHash,
        @Nullable Instant deadlineAt,
        @Nullable Instant publishedAt,
        @Nullable Instant completedAt,
        @Nullable Instant fundedAt,
        @Nullable String cancellationReason,
        @Nullable String depositTxHash,
        @Nullable String payoutTxHash,
        @Nullable String refundedTxHash,
        int version,
        @NonNull Instant createdAt,
        @NonNull Instant updatedAt) {
}
