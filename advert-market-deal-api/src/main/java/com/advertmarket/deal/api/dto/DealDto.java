package com.advertmarket.deal.api.dto;

import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Summary projection of a deal for list views.
 *
 * @param id deal identifier
 * @param channelId associated channel
 * @param advertiserId advertiser user ID
 * @param ownerId channel owner user ID
 * @param status current deal status
 * @param amountNano deal amount in nanoTON
 * @param deadlineAt current deadline (nullable)
 * @param createdAt creation timestamp
 * @param version optimistic lock version
 */
@Schema(description = "Deal summary")
public record DealDto(
        @NonNull DealId id,
        long channelId,
        long advertiserId,
        long ownerId,
        @NonNull DealStatus status,
        long amountNano,
        @Nullable Instant deadlineAt,
        @NonNull Instant createdAt,
        int version) {
}
