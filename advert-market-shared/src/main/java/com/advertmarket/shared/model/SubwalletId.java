package com.advertmarket.shared.model;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * TON subwallet identifier for escrow deal wallets.
 *
 * <p>Each deal gets a unique subwallet derived from the deal ID,
 * enabling per-deal deposit address generation via ton4j.
 */
public record SubwalletId(long value) {

    /**
     * Creates a subwallet identifier.
     *
     * @throws IllegalArgumentException if value is negative
     */
    public SubwalletId {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "SubwalletId must be non-negative, got: "
                            + value);
        }
    }

    @Override
    public @NonNull String toString() {
        return String.valueOf(value);
    }
}
