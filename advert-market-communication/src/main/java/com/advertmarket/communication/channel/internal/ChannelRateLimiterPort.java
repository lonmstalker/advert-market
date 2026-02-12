package com.advertmarket.communication.channel.internal;

/**
 * Port for rate limiting Telegram Channel API calls per channel.
 */
public interface ChannelRateLimiterPort {

    /**
     * Attempts to acquire a permit for the given channel.
     *
     * @param channelId the channel identifier
     * @return true if the request is allowed, false if throttled
     */
    boolean acquire(long channelId);
}
