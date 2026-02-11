package com.advertmarket.communication.bot.internal.state;

import java.time.Duration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Port for managing transient user state (e.g. "awaiting_creative").
 */
public interface UserStatePort {

    /**
     * Returns the current state for the given user, or null.
     *
     * @param userId the Telegram user id
     * @return the state string or null if none is set
     */
    @Nullable
    String getState(long userId);

    /**
     * Sets a state for the given user with an optional TTL.
     *
     * @param userId the Telegram user id
     * @param state  the state value
     * @param ttl    time-to-live, or null for the default
     */
    void setState(long userId, @NonNull String state,
            @Nullable Duration ttl);

    /**
     * Clears any state for the given user.
     *
     * @param userId the Telegram user id
     */
    void clearState(long userId);
}
