package com.advertmarket.financial.ledger.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import java.time.Duration;
import java.util.OptionalLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@DisplayName("RedisBalanceCache")
@ExtendWith(MockitoExtension.class)
class RedisBalanceCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private MetricsFacade metricsFacade;

    private RedisBalanceCache cache;

    private final AccountId accountId = AccountId.escrow(DealId.generate());

    @BeforeEach
    void setUp() {
        cache = new RedisBalanceCache(
                redisTemplate, metricsFacade, Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("Should return cached balance on valid value")
    void shouldReturnCachedBalance() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("5000");

        OptionalLong result = cache.get(accountId);

        assertThat(result).hasValue(5000L);
        verify(metricsFacade).incrementCounter(MetricNames.BALANCE_CACHE_HIT);
    }

    @Test
    @DisplayName("Should return empty on cache miss")
    void shouldReturnEmptyOnMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        OptionalLong result = cache.get(accountId);

        assertThat(result).isEmpty();
        verify(metricsFacade).incrementCounter(MetricNames.BALANCE_CACHE_MISS);
    }

    @Test
    @DisplayName("Should handle corrupted Redis value gracefully")
    void shouldHandleCorruptedRedisValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("not-a-number");

        OptionalLong result = cache.get(accountId);

        assertThat(result).isEmpty();
        verify(metricsFacade).incrementCounter(MetricNames.BALANCE_CACHE_MISS);
        verify(redisTemplate).delete("balance:" + accountId.value());
    }
}
