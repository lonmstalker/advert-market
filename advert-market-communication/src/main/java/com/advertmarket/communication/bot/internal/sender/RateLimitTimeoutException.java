package com.advertmarket.communication.bot.internal.sender;

import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;

/**
 * Thrown when acquiring a rate limiter permit times out.
 */
public class RateLimitTimeoutException extends DomainException {

    /** Creates exception with the given message. */
    public RateLimitTimeoutException(String message) {
        super(ErrorCodes.RATE_LIMIT_EXCEEDED, message);
    }
}