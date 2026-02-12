package com.advertmarket.shared.model;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * TON blockchain transaction hash.
 *
 * <p>Wraps a hex-encoded transaction hash string for type-safety
 * in financial operations and audit trails.
 */
public record TxHash(@NonNull String value) {

    /**
     * Creates a transaction hash.
     *
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is blank
     */
    public TxHash {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "TxHash must not be blank");
        }
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}
