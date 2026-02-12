package com.advertmarket.identity.adapter;

import com.advertmarket.identity.api.port.TokenBlacklistPort;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed JWT token blacklist.
 *
 * <p>Key pattern: {@code token:blacklist:{jti}} with TTL equal
 * to the remaining JWT lifetime.
 */
@Component
@RequiredArgsConstructor
public class RedisTokenBlacklistAdapter
        implements TokenBlacklistPort {

    private static final String KEY_PREFIX = "token:blacklist:";
    private static final String VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(@NonNull String jti,
            @NonNull Instant expiresAt) {
        Duration ttl = Duration.between(
                Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + jti, VALUE, ttl);
    }

    @Override
    public boolean isBlacklisted(@NonNull String jti) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
