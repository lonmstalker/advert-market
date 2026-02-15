package com.advertmarket.integration.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.advertmarket.financial.ledger.cache.RedisBalanceCache;
import com.advertmarket.integration.support.RedisSupport;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.UserId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.OptionalLong;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Integration tests for {@link RedisBalanceCache}.
 * Redis-only — no Spring context required.
 */
@DisplayName("RedisBalanceCache — Redis integration")
class RedisBalanceCacheIntegrationTest {

    private static StringRedisTemplate redisTemplate;
    private SimpleMeterRegistry meterRegistry;
    private MetricsFacade metricsFacade;
    private RedisBalanceCache cache;

    @BeforeAll
    static void initRedis() {
        redisTemplate = RedisSupport.redisTemplate();
    }

    @BeforeEach
    void setUp() {
        RedisSupport.flushAll();
        meterRegistry = new SimpleMeterRegistry();
        metricsFacade = new MetricsFacade(meterRegistry);
        cache = new RedisBalanceCache(
                redisTemplate, metricsFacade, Duration.ofMinutes(5));
    }

    @Nested
    @DisplayName("Basic operations")
    class BasicOperations {

        @Test
        @DisplayName("Should round-trip put and get")
        void putGetRoundTrip() {
            AccountId account = AccountId.escrow(DealId.generate());

            cache.put(account, 1_000_000_000L);
            OptionalLong result = cache.get(account);

            assertThat(result).hasValue(1_000_000_000L);
        }

        @Test
        @DisplayName("Should return empty on cache miss")
        void cacheMiss() {
            AccountId account = AccountId.escrow(DealId.generate());

            OptionalLong result = cache.get(account);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty after eviction")
        void eviction() {
            AccountId account = AccountId.escrow(DealId.generate());
            cache.put(account, 500L);

            cache.evict(account);

            assertThat(cache.get(account)).isEmpty();
        }

        @Test
        @DisplayName("Should overwrite existing value")
        void overwrite() {
            AccountId account = AccountId.escrow(DealId.generate());
            cache.put(account, 100L);

            cache.put(account, 200L);

            assertThat(cache.get(account)).hasValue(200L);
        }

        @Test
        @DisplayName("Should store negative balance for contra accounts")
        void negativeBalance() {
            AccountId externalTon = AccountId.externalTon();

            cache.put(externalTon, -3_000_000_000L);

            assertThat(cache.get(externalTon)).hasValue(-3_000_000_000L);
        }

        @Test
        @DisplayName("Should store zero balance")
        void zeroBalance() {
            AccountId account = AccountId.escrow(DealId.generate());

            cache.put(account, 0L);

            assertThat(cache.get(account)).hasValue(0L);
        }

        @Test
        @DisplayName("Should store Long.MAX_VALUE")
        void maxValue() {
            AccountId account = AccountId.platformTreasury();

            cache.put(account, Long.MAX_VALUE);

            assertThat(cache.get(account)).hasValue(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("Should store Long.MIN_VALUE")
        void minValue() {
            AccountId account = AccountId.externalTon();

            cache.put(account, Long.MIN_VALUE);

            assertThat(cache.get(account)).hasValue(Long.MIN_VALUE);
        }
    }

    @Nested
    @DisplayName("TTL behavior")
    class TtlBehavior {

        @Test
        @DisplayName("Should expire value after TTL")
        void expiresAfterTtl() throws InterruptedException {
            var shortTtlCache = new RedisBalanceCache(
                    redisTemplate, metricsFacade, Duration.ofSeconds(1));
            AccountId account = AccountId.escrow(DealId.generate());

            shortTtlCache.put(account, 42L);
            assertThat(shortTtlCache.get(account)).hasValue(42L);

            Thread.sleep(1_500L);

            assertThat(shortTtlCache.get(account)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Metrics tracking")
    class MetricsTracking {

        @Test
        @DisplayName("Should increment BALANCE_CACHE_HIT on hit")
        void cacheHitMetric() {
            AccountId account = AccountId.escrow(DealId.generate());
            cache.put(account, 100L);

            cache.get(account);

            Counter hitCounter = meterRegistry.find(
                    MetricNames.BALANCE_CACHE_HIT).counter();
            assertThat(hitCounter).isNotNull();
            assertThat(hitCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should increment BALANCE_CACHE_MISS on miss")
        void cacheMissMetric() {
            AccountId account = AccountId.escrow(DealId.generate());

            cache.get(account);

            Counter missCounter = meterRegistry.find(
                    MetricNames.BALANCE_CACHE_MISS).counter();
            assertThat(missCounter).isNotNull();
            assertThat(missCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should increment BALANCE_CACHE_MISS on corrupted value and evict")
        void corruptedValueMetric() {
            AccountId account = AccountId.escrow(DealId.generate());
            redisTemplate.opsForValue().set(
                    "balance:" + account.value(), "not-a-number");

            OptionalLong result = cache.get(account);

            assertThat(result).isEmpty();
            Counter missCounter = meterRegistry.find(
                    MetricNames.BALANCE_CACHE_MISS).counter();
            assertThat(missCounter).isNotNull();
            assertThat(missCounter.count()).isEqualTo(1.0);
            assertThat(cache.get(account)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Fail-open semantics")
    class FailOpenSemantics {

        @Test
        @DisplayName("Should return empty on Redis type mismatch instead of exception")
        void typeMismatch() {
            AccountId account = AccountId.escrow(DealId.generate());
            redisTemplate.opsForHash().put(
                    "balance:" + account.value(), "field", "value");

            OptionalLong result = cache.get(account);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should swallow DataAccessException on put when Redis key has wrong type")
        void putSwallowsDataAccessExceptionOnWrongType() {
            AccountId account = AccountId.escrow(DealId.generate());
            redisTemplate.opsForHash().put(
                    "balance:" + account.value(), "field", "value");

            assertThatCode(() -> cache.put(account, 123L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Key prefix")
    class KeyPrefix {

        @Test
        @DisplayName("Should use 'balance:' prefix in Redis key")
        void prefixUsed() {
            AccountId account = AccountId.escrow(DealId.generate());

            cache.put(account, 999L);

            String raw = redisTemplate.opsForValue().get(
                    "balance:" + account.value());
            assertThat(raw).isEqualTo("999");
        }

        @Test
        @DisplayName("Should isolate different account IDs")
        void accountIsolation() {
            AccountId account1 = AccountId.escrow(DealId.generate());
            AccountId account2 = AccountId.ownerPending(new UserId(99L));

            cache.put(account1, 100L);
            cache.put(account2, 200L);

            assertThat(cache.get(account1)).hasValue(100L);
            assertThat(cache.get(account2)).hasValue(200L);
        }
    }
}
