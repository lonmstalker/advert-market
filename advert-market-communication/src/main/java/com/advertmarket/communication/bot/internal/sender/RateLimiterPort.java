package com.advertmarket.communication.bot.internal.sender;

/**
 * Port for rate limiting outgoing Telegram Bot API calls.
 */
public interface RateLimiterPort {

    /**
     * Acquires permits for sending a message to the given chat.
     * Blocks the current thread until permits are available.
     *
     * @param chatId the target chat id
     */
    void acquire(long chatId);
}
