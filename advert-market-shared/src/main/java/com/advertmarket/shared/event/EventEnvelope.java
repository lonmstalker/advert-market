package com.advertmarket.shared.event;

import com.advertmarket.shared.model.DealId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Envelope wrapping a domain event with metadata.
 *
 * <p>The {@code payload} should be an immutable type
 * (preferably a Java record) to maintain envelope integrity.
 *
 * @param <T> the domain event type
 */
public record EventEnvelope<T extends DomainEvent>(
        @NonNull UUID eventId,
        @NonNull String eventType,
        @Nullable DealId dealId,
        @NonNull Instant timestamp,
        int version,
        @NonNull UUID correlationId,
        @NonNull T payload) {

    /**
     * Creates an event envelope.
     *
     * @throws NullPointerException if required fields are null
     * @throws IllegalArgumentException if version is less than 1
     */
    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(timestamp, "timestamp");
        if (version < 1) {
            throw new IllegalArgumentException(
                    "version must be >= 1, got: " + version);
        }
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(payload, "payload");
    }

    /**
     * Creates an envelope with generated IDs and current time.
     *
     * @param eventType the event type name
     * @param dealId optional deal identifier
     * @param payload the domain event payload
     * @param <T> the domain event type
     * @return new event envelope
     */
    public static <T extends DomainEvent> EventEnvelope<T> create(
            @NonNull String eventType,
            @Nullable DealId dealId,
            @NonNull T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                dealId,
                Instant.now(),
                1,
                UUID.randomUUID(),
                payload);
    }
}
