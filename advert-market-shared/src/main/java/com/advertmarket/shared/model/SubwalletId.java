package com.advertmarket.shared.model;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * TON subwallet identifier for escrow deal wallets.
 *
 * <p>Each deal gets a unique subwallet derived from the deal ID,
 * enabling per-deal deposit address generation via ton4j.
 */
public record SubwalletId(long value) {

    /** Maximum subwallet id (32-bit unsigned). */
    public static final long MAX_VALUE = 0xFFFFFFFFL;

    /**
     * Creates a subwallet identifier.
     *
     * @throws IllegalArgumentException if value is out of [0, 2^32-1]
     */
    public SubwalletId {
        if (value < 0 || value > MAX_VALUE) {
            throw new IllegalArgumentException(
                    "SubwalletId must be in [0, " + MAX_VALUE
                            + "], got: " + value);
        }
    }

    @Override
    public @NonNull String toString() {
        return String.valueOf(value);
    }
}
