package com.advertmarket.shared.model;

import java.util.Objects;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * TON blockchain transaction hash.
 *
 * <p>Wraps a hex-encoded or base64-encoded transaction hash
 * string for type-safety in financial operations and audit trails.
 */
public record TxHash(@NonNull String value) {

    private static final Pattern HEX_FORMAT =
            Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final Pattern BASE64_FORMAT =
            Pattern.compile("^[A-Za-z0-9+/=]{44}$");

    /**
     * Creates a transaction hash.
     *
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is blank or invalid
     */
    public TxHash {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "TxHash must not be blank");
        }
        if (!HEX_FORMAT.matcher(value).matches()
                && !BASE64_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid transaction hash format: " + value);
        }
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}
