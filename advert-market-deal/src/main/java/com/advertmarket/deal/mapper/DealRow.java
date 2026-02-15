package com.advertmarket.deal.mapper;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.JSON;

/**
 * JOOQ projection row for the {@code deals} table used to build {@code DealRecord}.
 */
public record DealRow(
        @NonNull UUID id,
        long channelId,
        long advertiserId,
        long ownerId,
        @Nullable Long pricingRuleId,
        @NonNull String status,
        long amountNano,
        int commissionRateBp,
        long commissionNano,
        @Nullable String depositAddress,
        @Nullable Integer subwalletId,
        @Nullable JSON creativeBrief,
        @Nullable JSON creativeDraft,
        @Nullable Long messageId,
        @Nullable String contentHash,
        @Nullable OffsetDateTime deadlineAt,
        @Nullable OffsetDateTime publishedAt,
        @Nullable OffsetDateTime completedAt,
        @Nullable OffsetDateTime fundedAt,
        @Nullable String cancellationReason,
        @Nullable String depositTxHash,
        @Nullable String payoutTxHash,
        @Nullable String refundedTxHash,
        int version,
        @NonNull OffsetDateTime createdAt,
        @NonNull OffsetDateTime updatedAt
) {
}
