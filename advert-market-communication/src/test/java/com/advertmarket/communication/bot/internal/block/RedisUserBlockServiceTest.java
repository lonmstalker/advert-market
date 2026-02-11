package com.advertmarket.communication.bot.internal.block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@DisplayName("RedisUserBlockService")
class RedisUserBlockServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private RedisUserBlockService service;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        var props = new UserBlockProperties("tg:block:");
        service = new RedisUserBlockService(redis, props);
    }

    @Test
    @DisplayName("isBlocked returns true when key exists")
    void isBlocked_trueWhenKeyExists() {
        when(redis.hasKey("tg:block:42")).thenReturn(true);

        assertThat(service.isBlocked(42L)).isTrue();
    }

    @Test
    @DisplayName("isBlocked returns false when key does not exist")
    void isBlocked_falseWhenKeyMissing() {
        when(redis.hasKey("tg:block:42")).thenReturn(false);

        assertThat(service.isBlocked(42L)).isFalse();
    }

    @Test
    @DisplayName("blockPermanently stores without TTL")
    void blockPermanently_storesWithoutTtl() {
        service.blockPermanently(42L, "spam");

        verify(valueOps).set("tg:block:42", "spam");
    }

    @Test
    @DisplayName("blockTemporarily stores with TTL")
    void blockTemporarily_storesWithTtl() {
        service.blockTemporarily(42L, "flood",
                Duration.ofHours(1));

        verify(valueOps).set("tg:block:42", "flood",
                Duration.ofHours(1));
    }

    @Test
    @DisplayName("unblock deletes the key")
    void unblock_deletesKey() {
        service.unblock(42L);

        verify(redis).delete("tg:block:42");
    }
}
