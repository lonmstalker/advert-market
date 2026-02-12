package com.advertmarket.identity.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@DisplayName("RedisTokenBlacklistAdapter — JWT blacklist via Redis")
class RedisTokenBlacklistAdapterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private RedisTokenBlacklistAdapter adapter;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        adapter = new RedisTokenBlacklistAdapter(redisTemplate);
    }

    @Test
    @DisplayName("Should blacklist token with correct key and TTL")
    void shouldBlacklistWithTtl() {
        String jti = "test-jti";
        Instant expiresAt = Instant.now().plusSeconds(3600);

        adapter.blacklist(jti, expiresAt);

        ArgumentCaptor<Duration> ttlCaptor =
                ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(
                eq("token:blacklist:" + jti),
                eq("1"),
                ttlCaptor.capture());
        assertThat(ttlCaptor.getValue().toSeconds())
                .isGreaterThan(3500);
    }

    @Test
    @DisplayName("Should skip blacklisting for already expired token")
    void shouldSkipExpiredToken() {
        adapter.blacklist("jti", Instant.now().minusSeconds(10));

        verify(valueOps, never()).set(
                anyString(), anyString(),
                org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    @DisplayName("Should return true when token is blacklisted")
    void shouldReturnTrueWhenBlacklisted() {
        when(redisTemplate.hasKey("token:blacklist:jti-1"))
                .thenReturn(true);

        assertThat(adapter.isBlacklisted("jti-1")).isTrue();
    }

    @Test
    @DisplayName("Should return false when token is not blacklisted")
    void shouldReturnFalseWhenNotBlacklisted() {
        when(redisTemplate.hasKey("token:blacklist:jti-2"))
                .thenReturn(false);

        assertThat(adapter.isBlacklisted("jti-2")).isFalse();
    }
}
