package com.advertmarket.communication.canary;

import com.advertmarket.shared.deploy.UserBucket;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Feature-flag canary router (Option B).
 * Determines whether a user should be routed to canary or stable code path.
 * Canary percent is stored in Redis and cached locally with a short TTL.
 */
@Slf4j
@Component
public class CanaryRouter {

    static final String REDIS_KEY = "canary:percent";
    static final String SALT_KEY = "canary:salt";
    static final int MAX_CANARY_PERCENT = 100;
    private static final long CACHE_TTL_MS = 5_000;

    private final StringRedisTemplate redis;
    private final MetricsFacade metrics;

    // Local cache to avoid hitting Redis on every request
    private final AtomicInteger cachedPercent = new AtomicInteger(0);
    private volatile String cachedSalt = "";
    private final AtomicLong lastFetchTime = new AtomicLong(0);

    /** Creates the canary router backed by Redis. */
    public CanaryRouter(StringRedisTemplate redis,
            MetricsFacade metrics) {
        this.redis = redis;
        this.metrics = metrics;
        metrics.registry().gauge(
                "canary.percent.current", cachedPercent);
    }

    /**
     * Determine if the given user should use canary path.
     */
    public boolean isCanary(long userId) {
        refreshCacheIfNeeded();
        int percent = cachedPercent.get();
        boolean canary = UserBucket.isCanary(userId, cachedSalt, percent);
        metrics.incrementCounter(
                MetricNames.CANARY_ROUTE_DECISION,
                "route", canary ? "canary" : "stable");
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
        if (percent < 0 || percent > MAX_CANARY_PERCENT) {
            throw new IllegalArgumentException(
                    "Canary percent must be between 0 and "
                            + MAX_CANARY_PERCENT + ", got: "
                            + percent);
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
            List<String> values = redis.opsForValue()
                    .multiGet(List.of(REDIS_KEY, SALT_KEY));
            if (values != null && values.size() == 2) {
                String percentStr = values.get(0);
                if (percentStr != null) {
                    cachedPercent.set(
                            Integer.parseInt(percentStr));
                }
                String salt = values.get(1);
                cachedSalt = salt != null ? salt : "";
            }
        } catch (Exception e) {
            log.warn("Failed to refresh canary config from "
                    + "Redis, using cached values", e);
        }
        lastFetchTime.set(now);
    }
}
