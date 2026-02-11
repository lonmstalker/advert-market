package com.advertmarket.communication.bot.internal.state;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed implementation of {@link UserStatePort}.
 */
@Slf4j
@Component
@EnableConfigurationProperties(UserStateProperties.class)
public class RedisUserStateService implements UserStatePort {

    private final StringRedisTemplate redis;
    private final UserStateProperties properties;

    /** Creates the state service backed by Redis. */
    public RedisUserStateService(StringRedisTemplate redis,
            UserStateProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    @Nullable
    public String getState(long userId) {
        return redis.opsForValue()
                .get(properties.keyPrefix() + userId);
    }

    @Override
    public void setState(long userId, @NonNull String state,
            @Nullable Duration ttl) {
        Duration effectiveTtl = ttl != null
                ? ttl : properties.defaultTtl();
        redis.opsForValue().set(
                properties.keyPrefix() + userId,
                state, effectiveTtl);
    }

    @Override
    public void clearState(long userId) {
        redis.delete(properties.keyPrefix() + userId);
    }
}
