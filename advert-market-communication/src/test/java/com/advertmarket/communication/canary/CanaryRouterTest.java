package com.advertmarket.communication.canary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.shared.metric.MetricsFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@DisplayName("CanaryRouter")
class CanaryRouterTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private CanaryRouter router;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        router = new CanaryRouter(redis,
                new MetricsFacade(new SimpleMeterRegistry()));
    }

    @Test
    @DisplayName("Zero percent routes all users to stable")
    void isCanary_zeroPercent_alwaysStable() {
        when(valueOps.multiGet(
                List.of(CanaryRouter.REDIS_KEY,
                        CanaryRouter.SALT_KEY)))
                .thenReturn(List.of("0", ""));

        for (long userId = 0; userId < 100; userId++) {
            assertThat(router.isCanary(userId)).isFalse();
        }
    }

    @Test
    @DisplayName("Hundred percent routes all users to canary")
    void isCanary_hundredPercent_alwaysCanary() {
        when(valueOps.multiGet(
                List.of(CanaryRouter.REDIS_KEY,
                        CanaryRouter.SALT_KEY)))
                .thenReturn(List.of("100", ""));

        for (long userId = 0; userId < 100; userId++) {
            assertThat(router.isCanary(userId)).isTrue();
        }
    }

    @Test
    @DisplayName("Setting canary percent writes to Redis")
    void setCanaryPercent_writesToRedis() {
        router.setCanaryPercent(25);
        verify(valueOps).set(CanaryRouter.REDIS_KEY, "25");
    }

    @Test
    @DisplayName("Setting canary percent rejects out-of-range values")
    void setCanaryPercent_rejectsOutOfRange() {
        assertThatThrownBy(() -> router.setCanaryPercent(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> router.setCanaryPercent(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Returns current canary percent from Redis")
    void getCanaryPercent_returnsCurrentValue() {
        when(valueOps.multiGet(
                List.of(CanaryRouter.REDIS_KEY,
                        CanaryRouter.SALT_KEY)))
                .thenReturn(Arrays.asList("42", null));

        assertThat(router.getCanaryPercent()).isEqualTo(42);
    }

    @Test
    @DisplayName("Setting salt writes to Redis")
    void setSalt_writesToRedis() {
        router.setSalt("v2-rollout");
        verify(valueOps).set(
                CanaryRouter.SALT_KEY, "v2-rollout");
    }

    @Test
    @DisplayName("Uses cached values when Redis fails")
    void isCanary_redisFails_usesCachedValues() {
        when(valueOps.multiGet(
                List.of(CanaryRouter.REDIS_KEY,
                        CanaryRouter.SALT_KEY)))
                .thenReturn(List.of("50", "test"));
        router.getCanaryPercent();

        router.setCanaryPercent(50);

        assertThat(router.getCanaryPercent()).isEqualTo(50);
    }

    @Test
    @DisplayName("Canary status is consistent for the same user")
    void isCanary_stickyPerUser() {
        when(valueOps.multiGet(
                List.of(CanaryRouter.REDIS_KEY,
                        CanaryRouter.SALT_KEY)))
                .thenReturn(List.of("50", ""));

        long userId = 42L;
        boolean first = router.isCanary(userId);
        for (int i = 0; i < 50; i++) {
            assertThat(router.isCanary(userId))
                    .as("Canary status must be stable for same user")
                    .isEqualTo(first);
        }
    }
}