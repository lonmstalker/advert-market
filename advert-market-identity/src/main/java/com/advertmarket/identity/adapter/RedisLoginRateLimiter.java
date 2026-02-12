package com.advertmarket.identity.adapter;

import com.advertmarket.identity.api.port.LoginRateLimiterPort;
import com.advertmarket.identity.config.RateLimiterProperties;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * IP-based rate limiter for the login endpoint using an atomic
 * Redis Lua script (INCR + conditional EXPIRE in one round-trip).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLoginRateLimiter implements LoginRateLimiterPort {

    private static final String KEY_PREFIX = "rate:login:";

    private static final RedisScript<Long> INCREMENT_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
                    return count
                    """,
                    Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterProperties properties;
    private final MetricsFacade metricsFacade;

    @Override
    public void checkRate(@NonNull String clientIp) {
        try {
            String key = KEY_PREFIX + clientIp;
            Long count = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(key),
                    String.valueOf(properties.windowSeconds()));
            if (count != null && count > properties.maxAttempts()) {
                throw new DomainException(
                        ErrorCodes.RATE_LIMIT_EXCEEDED,
                        "Too many login attempts");
            }
        } catch (DataAccessException e) {
            metricsFacade.incrementCounter(
                    MetricNames.AUTH_RATE_LIMITER_REDIS_ERROR);
            log.error("Rate limiter Redis error, failing closed", e);
            throw new DomainException(
                    ErrorCodes.SERVICE_UNAVAILABLE,
                    "Rate limiting unavailable");
        }
    }
}
