package com.advertmarket.communication.bot.internal.block;

import com.advertmarket.shared.model.UserBlockCheckPort;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed implementation of {@link UserBlockPort} and {@link UserBlockCheckPort}.
 */
@RequiredArgsConstructor
@Slf4j
@Component
@EnableConfigurationProperties(UserBlockProperties.class)
public class RedisUserBlockService implements UserBlockPort, UserBlockCheckPort {

    private final StringRedisTemplate redis;
    private final UserBlockProperties properties;

    @Override
    public boolean isBlocked(long userId) {
        try {
            return Boolean.TRUE.equals(
                    redis.hasKey(properties.keyPrefix() + userId));
        } catch (DataAccessException e) {
            log.warn("Redis error checking block for userId={}",
                    userId, e);
            return false;
        }
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
