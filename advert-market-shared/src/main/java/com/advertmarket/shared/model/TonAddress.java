package com.advertmarket.shared.model;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * TON blockchain wallet address.
 *
 * <p>Supports both raw ({@code 0:...}) and user-friendly
 * ({@code UQ...}/{@code EQ...}) address formats.
 */
public record TonAddress(@NonNull String value) {

    /**
     * Creates a TON address.
     *
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is blank
     */
    public TonAddress {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "TonAddress must not be blank");
        }
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}
