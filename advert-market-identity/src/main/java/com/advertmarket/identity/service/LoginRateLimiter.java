package com.advertmarket.identity.service;

import com.advertmarket.shared.exception.DomainException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * IP-based rate limiter for the login endpoint using Redis INCR.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "rate:login:";
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    /**
     * Checks whether the client IP has exceeded the login rate limit.
     *
     * @param clientIp the client IP address
     * @throws DomainException if rate limit exceeded
     */
    public void checkRate(@NonNull String clientIp) {
        try {
            String key = KEY_PREFIX + clientIp;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, WINDOW);
            }
            if (count != null && count > MAX_ATTEMPTS) {
                throw new DomainException(
                        "RATE_LIMIT_EXCEEDED",
                        "Too many login attempts");
            }
        } catch (DataAccessException e) {
            log.warn("Rate limiter Redis error, allowing request", e);
        }
    }
}
