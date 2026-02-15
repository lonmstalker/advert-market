package com.advertmarket.deal.mapper;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.JSONB;

/**
 * JOOQ projection row for the {@code deal_events} table used to build {@code DealEventRecord}.
 */
public record DealEventRow(
        @Nullable Long id,
        @NonNull UUID dealId,
        @NonNull String eventType,
        @Nullable String fromStatus,
        @Nullable String toStatus,
        @Nullable Long actorId,
        @NonNull String actorType,
        @Nullable JSONB payload,
        @NonNull OffsetDateTime createdAt
) {
}
