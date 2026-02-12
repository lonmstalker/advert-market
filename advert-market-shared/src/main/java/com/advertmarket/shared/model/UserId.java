package com.advertmarket.shared.model;

import java.io.Serializable;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Telegram user identifier (always positive).
 */
public record UserId(long value) implements Serializable {

    /**
     * Creates a user identifier.
     *
     * @throws IllegalArgumentException if value is not positive
     */
    public UserId {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "UserId must be positive, got: " + value);
        }
    }

    @Override
    public @NonNull String toString() {
        return String.valueOf(value);
    }
}
