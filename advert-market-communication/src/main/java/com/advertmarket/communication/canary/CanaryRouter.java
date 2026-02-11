package com.advertmarket.communication.canary;

import com.advertmarket.shared.deploy.UserBucket;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Feature-flag canary router (Option B).
 * Determines whether a user should be routed to canary or stable code path.
 * Canary percent is stored in Redis and cached locally with a short TTL.
 */
@Component
public class CanaryRouter {

    private static final Logger log = LoggerFactory.getLogger(CanaryRouter.class);

    static final String REDIS_KEY = "canary:percent";
    static final String SALT_KEY = "canary:salt";
    private static final long CACHE_TTL_MS = 5_000; // 5 seconds

    private final StringRedisTemplate redis;
    private final Counter stableCounter;
    private final Counter canaryCounter;

    // Local cache to avoid hitting Redis on every request
    private final AtomicInteger cachedPercent = new AtomicInteger(0);
    private volatile String cachedSalt = "";
    private final AtomicLong lastFetchTime = new AtomicLong(0);

    public CanaryRouter(StringRedisTemplate redis, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.stableCounter = Counter.builder("canary.route.decision")
                .tag("route", "stable")
                .register(meterRegistry);
        this.canaryCounter = Counter.builder("canary.route.decision")
                .tag("route", "canary")
                .register(meterRegistry);
        meterRegistry.gauge("canary.percent.current", cachedPercent);
    }

    /**
     * Determine if the given user should use canary path.
     */
    public boolean isCanary(long userId) {
        refreshCacheIfNeeded();
        int percent = cachedPercent.get();
        boolean canary = UserBucket.isCanary(userId, cachedSalt, percent);
        if (canary) {
            canaryCounter.increment();
        } else {
            stableCounter.increment();
        }
        return canary;
    }

    /**
     * Get current canary percent (for admin API).
     */
    public int getCanaryPercent() {
        refreshCacheIfNeeded();
        return cachedPercent.get();
    }

    /**
     * Set canary percent (for admin API). Writes to Redis.
     */
    public void setCanaryPercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Canary percent must be between 0 and 100, got: " + percent);
        }
        redis.opsForValue().set(REDIS_KEY, String.valueOf(percent));
        cachedPercent.set(percent);
        lastFetchTime.set(System.currentTimeMillis());
        log.info("Canary percent updated to {}%", percent);
    }

    /**
     * Get current salt.
     */
    public String getSalt() {
        refreshCacheIfNeeded();
        return cachedSalt;
    }

    /**
     * Set salt (redistributes buckets).
     */
    public void setSalt(String salt) {
        redis.opsForValue().set(SALT_KEY, salt != null ? salt : "");
        cachedSalt = salt != null ? salt : "";
        lastFetchTime.set(System.currentTimeMillis());
        log.info("Canary salt updated");
    }

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime.get() < CACHE_TTL_MS) {
            return;
        }
        try {
            String percentStr = redis.opsForValue().get(REDIS_KEY);
            if (percentStr != null) {
                cachedPercent.set(Integer.parseInt(percentStr));
            }
            String salt = redis.opsForValue().get(SALT_KEY);
            cachedSalt = salt != null ? salt : "";
        } catch (Exception e) {
            log.warn("Failed to refresh canary config from Redis, using cached values", e);
        }
        lastFetchTime.set(now);
    }
}