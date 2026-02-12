package com.advertmarket.communication.channel.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@DisplayName("RedisChannelRateLimiter")
class RedisChannelRateLimiterTest {

    private ValueOperations<String, String> valueOps;
    private RedisChannelRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        var redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        rateLimiter = new RedisChannelRateLimiter(redis);
    }

    @Test
    @DisplayName("Returns true when key was set (not throttled)")
    void acquire_returnsTrue_whenKeySet() {
        when(valueOps.setIfAbsent(
                "tg:chan:rl:123", "1", Duration.ofSeconds(1)))
                .thenReturn(true);

        assertThat(rateLimiter.acquire(123L)).isTrue();
    }

    @Test
    @DisplayName("Returns false when key already exists (throttled)")
    void acquire_returnsFalse_whenKeyExists() {
        when(valueOps.setIfAbsent(
                "tg:chan:rl:123", "1", Duration.ofSeconds(1)))
                .thenReturn(false);

        assertThat(rateLimiter.acquire(123L)).isFalse();
    }

    @Test
    @DisplayName("Returns true on Redis error (fail-open)")
    void acquire_failOpen_onRedisError() {
        when(valueOps.setIfAbsent(
                anyString(), anyString(), any(Duration.class)))
                .thenThrow(new TestDataAccessException("fail"));

        assertThat(rateLimiter.acquire(123L)).isTrue();
    }

    private static class TestDataAccessException
            extends DataAccessException {
        TestDataAccessException(String msg) {
            super(msg);
        }
    }
}
