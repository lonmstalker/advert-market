package com.advertmarket.financial.ledger.cache;

import com.advertmarket.financial.api.port.BalanceCachePort;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import java.time.Duration;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis-based balance cache with fail-open semantics.
 *
 * <p>If Redis is unavailable, methods return cache miss / silently fail,
 * allowing the caller to fall back to the database.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisBalanceCache implements BalanceCachePort {

    private static final String KEY_PREFIX = "balance:";

    private final StringRedisTemplate redisTemplate;
    private final MetricsFacade metricsFacade;
    private final Duration cacheTtl;

    @Override
    public @NonNull OptionalLong get(@NonNull AccountId accountId) {
        try {
            String value = redisTemplate.opsForValue()
                    .get(KEY_PREFIX + accountId.value());
            if (value != null) {
                metricsFacade.incrementCounter(MetricNames.BALANCE_CACHE_HIT);
                return OptionalLong.of(Long.parseLong(value));
            }
            metricsFacade.incrementCounter(MetricNames.BALANCE_CACHE_MISS);
            return OptionalLong.empty();
        } catch (DataAccessException ex) {
            log.warn("Redis balance cache get failed for {}", accountId, ex);
            metricsFacade.incrementCounter(MetricNames.BALANCE_CACHE_MISS);
            return OptionalLong.empty();
        }
    }

    @Override
    public void put(@NonNull AccountId accountId, long balanceNano) {
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + accountId.value(),
                    String.valueOf(balanceNano),
                    cacheTtl);
        } catch (DataAccessException ex) {
            log.warn("Redis balance cache put failed for {}", accountId, ex);
        }
    }

    @Override
    public void evict(@NonNull AccountId accountId) {
        try {
            redisTemplate.delete(KEY_PREFIX + accountId.value());
        } catch (DataAccessException ex) {
            log.warn("Redis balance cache evict failed for {}", accountId, ex);
        }
    }
}
