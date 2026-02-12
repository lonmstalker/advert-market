package com.advertmarket.shared.model;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Telegram channel identifier (non-zero, typically negative).
 */
public record ChannelId(long value) {

    /**
     * Creates a channel identifier.
     *
     * @throws IllegalArgumentException if value is zero
     */
    public ChannelId {
        if (value == 0) {
            throw new IllegalArgumentException(
                    "ChannelId must be non-zero");
        }
    }

    @Override
    public @NonNull String toString() {
        return String.valueOf(value);
    }
}
