package com.advertmarket.shared.lock;

import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis-backed implementation of {@link DistributedLockPort}.
 *
 * <p>Uses {@code SET NX PX} for lock acquisition and a Lua script
 * for compare-and-delete on unlock. Fail-open: Redis errors result
 * in an empty optional (lock not acquired) with a WARN log.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisDistributedLock implements DistributedLockPort {

    private static final String KEY_PREFIX = "lock:";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    end
                    return 0
                    """,
                    Long.class);

    private final StringRedisTemplate redisTemplate;
    private final MetricsFacade metrics;

    @Override
    public @NonNull Optional<String> tryLock(
            @NonNull String key, @NonNull Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(ttl, "ttl");
        if (key.isBlank()) {
            throw new IllegalArgumentException(
                    "Lock key must not be blank");
        }
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException(
                    "Lock TTL must be positive, got: " + ttl);
        }

        String token = UUID.randomUUID().toString();
        String redisKey = KEY_PREFIX + key;

        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, token, ttl);
            if (Boolean.TRUE.equals(acquired)) {
                metrics.incrementCounter(MetricNames.LOCK_ACQUIRED,
                        "key", key);
                return Optional.of(token);
            }
            metrics.incrementCounter(MetricNames.LOCK_TIMEOUT,
                    "key", key);
            return Optional.empty();
        } catch (DataAccessException ex) {
            log.warn("Redis error acquiring lock '{}': {}",
                    key, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void unlock(
            @NonNull String key, @NonNull String token) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(token, "token");

        String redisKey = KEY_PREFIX + key;
        try {
            redisTemplate.execute(UNLOCK_SCRIPT,
                    java.util.List.of(redisKey), token);
        } catch (DataAccessException ex) {
            log.warn("Redis error releasing lock '{}': {}",
                    key, ex.getMessage());
        }
    }
}
