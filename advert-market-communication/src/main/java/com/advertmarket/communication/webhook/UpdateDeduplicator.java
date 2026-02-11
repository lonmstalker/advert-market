package com.advertmarket.communication.webhook;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Deduplicates Telegram updates by update_id using Redis SET NX.
 * TTL ensures old entries are cleaned up automatically.
 */
@Component
public class UpdateDeduplicator implements UpdateDeduplicationPort {

    private static final String KEY_PREFIX = "tg:update:";

    private final StringRedisTemplate redis;
    private final DeduplicationProperties properties;
    private final Counter acquiredCounter;

    /** Creates the deduplicator backed by Redis. */
    public UpdateDeduplicator(StringRedisTemplate redis,
            DeduplicationProperties properties,
            MeterRegistry meterRegistry) {
        this.redis = redis;
        this.properties = properties;
        this.acquiredCounter = Counter
                .builder("telegram.update.dedup.acquired")
                .description("Successfully acquired (non-duplicate) "
                        + "updates")
                .register(meterRegistry);
    }

    @Override
    public boolean tryAcquire(int updateId) {
        String key = KEY_PREFIX + updateId;
        Boolean set = redis.opsForValue()
                .setIfAbsent(key, "1", properties.ttl());
        if (Boolean.TRUE.equals(set)) {
            acquiredCounter.increment();
            return true;
        }
        return false;
    }
}
