package com.advertmarket.deal.api.dto;

import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Full deal detail including event timeline.
 *
 * @param id deal identifier
 * @param channelId associated channel
 * @param advertiserId advertiser user ID
 * @param ownerId channel owner user ID
 * @param status current deal status
 * @param amountNano deal amount in nanoTON
 * @param commissionRateBp commission rate in basis points
 * @param commissionNano commission amount in nanoTON
 * @param deadlineAt current deadline (nullable)
 * @param createdAt creation timestamp
 * @param version optimistic lock version
 * @param timeline list of deal events ordered by time descending
 */
@Schema(description = "Deal detail with event timeline")
public record DealDetailDto(
        @NonNull DealId id,
        long channelId,
        long advertiserId,
        long ownerId,
        @NonNull DealStatus status,
        long amountNano,
        int commissionRateBp,
        long commissionNano,
        @Nullable Instant deadlineAt,
        @NonNull Instant createdAt,
        int version,
        @NonNull List<DealEventDto> timeline) {
}
