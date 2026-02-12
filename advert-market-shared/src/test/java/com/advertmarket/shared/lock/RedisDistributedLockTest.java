package com.advertmarket.shared.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@DisplayName("RedisDistributedLock")
@ExtendWith(MockitoExtension.class)
class RedisDistributedLockTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private MetricsFacade metrics;

    @InjectMocks
    private RedisDistributedLock lock;

    @Test
    @DisplayName("tryLock returns token when lock acquired")
    void tryLock_acquired_returnsToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(
                eq("lock:deal:123"),
                anyString(),
                eq(Duration.ofSeconds(30))))
                .thenReturn(true);

        Optional<String> result =
                lock.tryLock("deal:123", Duration.ofSeconds(30));

        assertThat(result).isPresent();
        verify(metrics).incrementCounter(
                eq(MetricNames.LOCK_ACQUIRED),
                eq("namespace"), eq("deal"));
    }

    @Test
    @DisplayName("tryLock returns empty when lock not acquired")
    void tryLock_notAcquired_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(
                anyString(), anyString(), any(Duration.class)))
                .thenReturn(false);

        Optional<String> result =
                lock.tryLock("deal:123", Duration.ofSeconds(30));

        assertThat(result).isEmpty();
        verify(metrics).incrementCounter(
                eq(MetricNames.LOCK_TIMEOUT),
                eq("namespace"), eq("deal"));
    }

    @Test
    @DisplayName("tryLock returns empty on Redis error (fail-open)")
    void tryLock_redisError_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(
                anyString(), anyString(), any(Duration.class)))
                .thenThrow(new QueryTimeoutException("timeout"));

        Optional<String> result =
                lock.tryLock("deal:123", Duration.ofSeconds(30));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("tryLock rejects blank key")
    void tryLock_blankKey_throws() {
        assertThatThrownBy(() ->
                lock.tryLock("  ", Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("tryLock rejects negative TTL")
    void tryLock_negativeTtl_throws() {
        assertThatThrownBy(() ->
                lock.tryLock("key", Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("tryLock rejects zero TTL")
    void tryLock_zeroTtl_throws() {
        assertThatThrownBy(() ->
                lock.tryLock("key", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("unlock executes Lua script")
    void unlock_executesScript() {
        lock.unlock("deal:123", "token-abc");

        verify(redisTemplate).execute(
                any(), eq(List.of("lock:deal:123")),
                eq("token-abc"));
    }

    @Test
    @DisplayName("unlock swallows Redis error")
    void unlock_redisError_swallowed() {
        when(redisTemplate.execute(
                any(), any(), anyString()))
                .thenThrow(new QueryTimeoutException("timeout"));

        // should not throw
        lock.unlock("deal:123", "token-abc");
    }
}
