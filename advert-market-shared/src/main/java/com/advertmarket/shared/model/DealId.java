package com.advertmarket.shared.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Unique identifier for a deal.
 */
public record DealId(@NonNull UUID value) {

    /**
     * Creates a deal identifier.
     *
     * @throws NullPointerException if value is null
     */
    public DealId {
        Objects.requireNonNull(value, "value");
    }

    /** Generates a new random deal identifier. */
    public static @NonNull DealId generate() {
        return new DealId(UUID.randomUUID());
    }

    /**
     * Creates a deal identifier from a UUID.
     *
     * @param value the UUID
     * @return deal identifier
     */
    public static @NonNull DealId of(@NonNull UUID value) {
        return new DealId(value);
    }

    /**
     * Creates a deal identifier from a string.
     *
     * @param value UUID string representation
     * @return deal identifier
     * @throws IllegalArgumentException if not a valid UUID
     */
    @JsonCreator
    public static @NonNull DealId of(@NonNull String value) {
        return new DealId(UUID.fromString(value));
    }

    /** Returns the UUID value for JSON serialization. */
    @JsonValue
    @Override
    public @NonNull UUID value() {
        return value;
    }

    @Override
    public @NonNull String toString() {
        return value.toString();
    }
}
