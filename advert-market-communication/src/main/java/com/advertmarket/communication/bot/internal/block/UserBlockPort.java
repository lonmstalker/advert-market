package com.advertmarket.communication.bot.internal.block;

import java.time.Duration;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for blocking and unblocking users from the bot.
 */
public interface UserBlockPort {

    /**
     * Returns true if the given user is currently blocked.
     *
     * @param userId the Telegram user id
     * @return true if blocked
     */
    boolean isBlocked(long userId);

    /**
     * Blocks a user permanently.
     *
     * @param userId the Telegram user id
     * @param reason human-readable block reason
     */
    void blockPermanently(long userId, @NonNull String reason);

    /**
     * Blocks a user for a limited duration.
     *
     * @param userId   the Telegram user id
     * @param reason   human-readable block reason
     * @param duration how long to block
     */
    void blockTemporarily(long userId, @NonNull String reason,
            Duration duration);

    /**
     * Unblocks the given user.
     *
     * @param userId the Telegram user id
     */
    void unblock(long userId);
}
