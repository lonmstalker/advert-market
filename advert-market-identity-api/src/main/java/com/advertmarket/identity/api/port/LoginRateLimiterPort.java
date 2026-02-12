package com.advertmarket.identity.api.port;

import com.advertmarket.shared.exception.DomainException;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Rate limiter for login attempts.
 */
public interface LoginRateLimiterPort {

    /**
     * Checks whether the client IP has exceeded the login rate limit.
     *
     * @param clientIp the client IP address
     * @throws DomainException if rate limit exceeded
     */
    void checkRate(@NonNull String clientIp);
}
