package com.advertmarket.shared.event;

import com.advertmarket.shared.FenumGroup;
import com.advertmarket.shared.model.DealId;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.UUID;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * HTTP fallback DTO for worker callback events.
 *
 * <p>Used by {@code POST /internal/v1/worker-events}
 * when Kafka is unavailable or for debug/manual replay.
 *
 * @param callbackType the event type discriminator
 * @param dealId optional deal identifier
 * @param correlationId correlation identifier
 * @param payload raw JSON payload resolved by callbackType
 */
public record WorkerCallback(
        @Fenum(FenumGroup.EVENT_TYPE) @NonNull String callbackType,
        @Nullable DealId dealId,
        @NonNull UUID correlationId,
        @NonNull JsonNode payload) {

    /**
     * Creates a worker callback with validation.
     *
     * @throws NullPointerException if required fields are null
     */
    public WorkerCallback {
        Objects.requireNonNull(callbackType, "callbackType");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(payload, "payload");
    }
}
