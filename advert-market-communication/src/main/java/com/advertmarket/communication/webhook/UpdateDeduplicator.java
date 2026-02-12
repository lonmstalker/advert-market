package com.advertmarket.communication.webhook;

import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Deduplicates Telegram updates by update_id using Redis SET NX.
 * TTL ensures old entries are cleaned up automatically.
 */
@Slf4j
@Component
public class UpdateDeduplicator implements UpdateDeduplicationPort {

    private static final String KEY_PREFIX = "tg:update:";

    private final StringRedisTemplate redis;
    private final DeduplicationProperties properties;
    private final MetricsFacade metrics;

    /** Creates the deduplicator backed by Redis. */
    public UpdateDeduplicator(StringRedisTemplate redis,
            DeduplicationProperties properties,
            MetricsFacade metrics) {
        this.redis = redis;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Override
    public boolean tryAcquire(int updateId) {
        try {
            String key = KEY_PREFIX + updateId;
            Boolean set = redis.opsForValue()
                    .setIfAbsent(key, "1", properties.ttl());
            if (Boolean.TRUE.equals(set)) {
                metrics.incrementCounter(
                        MetricNames.DEDUP_ACQUIRED);
                return true;
            }
            return false;
        } catch (DataAccessException e) {
            log.warn("Redis error during deduplication for "
                    + "updateId={}, allowing through",
                    updateId, e);
            return true;
        }
    }
}
