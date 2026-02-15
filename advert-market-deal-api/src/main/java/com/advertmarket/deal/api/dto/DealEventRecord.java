package com.advertmarket.deal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Internal persistence projection matching the {@code deal_events} table.
 *
 * @param id auto-generated event ID
 * @param dealId associated deal UUID
 * @param eventType event type code
 * @param fromStatus previous status (nullable)
 * @param toStatus new status (nullable)
 * @param actorId initiating user ID (nullable)
 * @param actorType actor type string
 * @param payload event payload as JSON string
 * @param createdAt event timestamp
 */
@Schema(description = "Deal event persistence record")
public record DealEventRecord(
        @Nullable Long id,
        @NonNull UUID dealId,
        @NonNull String eventType,
        @Nullable String fromStatus,
        @Nullable String toStatus,
        @Nullable Long actorId,
        @NonNull String actorType,
        @NonNull String payload,
        @NonNull Instant createdAt) {
}
