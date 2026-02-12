package com.advertmarket.shared.model;

import java.util.Objects;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * TON blockchain wallet address.
 *
 * <p>Supports both raw ({@code 0:...}) and user-friendly
 * ({@code UQ...}/{@code EQ...}) address formats.
 */
public record TonAddress(@NonNull String value) {

    private static final Pattern RAW_FORMAT =
            Pattern.compile("^-?[0-9]+:[0-9a-fA-F]{64}$");
    private static final Pattern USER_FRIENDLY_FORMAT =
            Pattern.compile("^[A-Za-z0-9_-]{48}$");

    /**
     * Creates a TON address.
     *
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is blank or invalid
     */
    public TonAddress {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "TonAddress must not be blank");
        }
        if (!RAW_FORMAT.matcher(value).matches()
                && !USER_FRIENDLY_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid TON address format: " + value);
        }
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}
