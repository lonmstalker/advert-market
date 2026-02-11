package com.advertmarket.communication.bot.internal.block;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed implementation of {@link UserBlockPort}.
 */
@Slf4j
@Component
@EnableConfigurationProperties(UserBlockProperties.class)
public class RedisUserBlockService implements UserBlockPort {

    private final StringRedisTemplate redis;
    private final UserBlockProperties properties;

    /** Creates the block service backed by Redis. */
    public RedisUserBlockService(StringRedisTemplate redis,
            UserBlockProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public boolean isBlocked(long userId) {
        return Boolean.TRUE.equals(
                redis.hasKey(properties.keyPrefix() + userId));
    }

    @Override
    public void blockPermanently(long userId,
            @NonNull String reason) {
        redis.opsForValue().set(
                properties.keyPrefix() + userId, reason);
        log.info("User {} blocked permanently: {}",
                userId, reason);
    }

    @Override
    public void blockTemporarily(long userId,
            @NonNull String reason, Duration duration) {
        redis.opsForValue().set(
                properties.keyPrefix() + userId,
                reason, duration);
        log.info("User {} blocked for {}: {}",
                userId, duration, reason);
    }

    @Override
    public void unblock(long userId) {
        redis.delete(properties.keyPrefix() + userId);
        log.info("User {} unblocked", userId);
    }
}
