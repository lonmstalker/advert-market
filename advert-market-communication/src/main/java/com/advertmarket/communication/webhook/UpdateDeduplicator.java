package com.advertmarket.communication.webhook;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Deduplicates Telegram updates by update_id using Redis SET NX.
 * TTL ensures old entries are cleaned up automatically.
 */
@Component
public class UpdateDeduplicator {

    private static final String KEY_PREFIX = "tg:update:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final Counter acquiredCounter;

    public UpdateDeduplicator(StringRedisTemplate redis, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.acquiredCounter = Counter.builder("telegram.update.dedup.acquired")
                .description("Successfully acquired (non-duplicate) updates")
                .register(meterRegistry);
    }

    /**
     * Try to mark update_id as processed.
     *
     * @param updateId Telegram update_id
     * @return true if this is the first time we see this update_id (not a duplicate)
     */
    public boolean tryAcquire(int updateId) {
        String key = KEY_PREFIX + updateId;
        Boolean set = redis.opsForValue().setIfAbsent(key, "1", TTL);
        if (Boolean.TRUE.equals(set)) {
            acquiredCounter.increment();
            return true;
        }
        return false;
    }
}
