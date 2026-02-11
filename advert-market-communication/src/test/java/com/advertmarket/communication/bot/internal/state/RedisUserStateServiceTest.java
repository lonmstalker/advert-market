package com.advertmarket.communication.bot.internal.state;

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

@DisplayName("RedisUserStateService")
class RedisUserStateServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private RedisUserStateService service;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        var props = new UserStateProperties(
                Duration.ofHours(1), "tg:state:");
        service = new RedisUserStateService(redis, props);
    }

    @Test
    @DisplayName("getState returns value from Redis")
    void getState_returnsValue() {
        when(valueOps.get("tg:state:42"))
                .thenReturn("awaiting_creative");

        assertThat(service.getState(42L))
                .isEqualTo("awaiting_creative");
    }

    @Test
    @DisplayName("getState returns null for non-existent key")
    void getState_returnsNullForMissing() {
        when(valueOps.get("tg:state:42")).thenReturn(null);

        assertThat(service.getState(42L)).isNull();
    }

    @Test
    @DisplayName("setState with custom TTL")
    void setState_withCustomTtl() {
        service.setState(42L, "confirming",
                Duration.ofMinutes(30));

        verify(valueOps).set("tg:state:42", "confirming",
                Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("setState with default TTL from properties")
    void setState_withDefaultTtl() {
        service.setState(42L, "editing", null);

        verify(valueOps).set("tg:state:42", "editing",
                Duration.ofHours(1));
    }

    @Test
    @DisplayName("clearState deletes the key")
    void clearState_deletesKey() {
        service.clearState(42L);

        verify(redis).delete("tg:state:42");
    }
}
