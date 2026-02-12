package com.advertmarket.communication.bot.internal.sender;

/**
 * Thrown when acquiring a rate limiter permit times out.
 */
public class RateLimitTimeoutException extends RuntimeException {

    public RateLimitTimeoutException(String message) {
        super(message);
    }
}