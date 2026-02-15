package com.advertmarket.deal.api.dto;

import com.advertmarket.shared.model.DealStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Projection of a deal event for the timeline view.
 *
 * @param id event database ID
 * @param eventType event type code
 * @param fromStatus previous status (nullable for creation events)
 * @param toStatus new status (nullable for non-transition events)
 * @param actorId initiating user ID (null for SYSTEM)
 * @param createdAt event timestamp
 */
@Schema(description = "Deal event")
public record DealEventDto(
        long id,
        @NonNull String eventType,
        @Nullable DealStatus fromStatus,
        @Nullable DealStatus toStatus,
        @Nullable Long actorId,
        @NonNull Instant createdAt) {
}
