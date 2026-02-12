package com.advertmarket.identity.adapter;

import com.advertmarket.identity.api.port.TokenBlacklistPort;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed token blacklist.
 *
 * <p>Fail-closed: if Redis is unreachable, {@link #isBlacklisted}
 * returns {@code true} to prevent revoked tokens from being accepted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTokenBlacklist implements TokenBlacklistPort {

    private static final String KEY_PREFIX = "jwt:blacklist:";
    private static final String VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean isBlacklisted(@NonNull String jti) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey(KEY_PREFIX + jti));
        } catch (DataAccessException e) {
            log.error("Redis error checking token blacklist,"
                    + " failing closed for jti={}", jti, e);
            return true;
        }
    }

    @Override
    public void blacklist(@NonNull String jti, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + jti, VALUE,
                    Duration.ofSeconds(ttlSeconds));
        } catch (DataAccessException e) {
            log.error("Redis error blacklisting token jti={}",
                    jti, e);
        }
    }
}
