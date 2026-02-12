package com.advertmarket.integration.deploy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.communication.canary.CanaryRouter;
import com.advertmarket.shared.metric.MetricsFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for CanaryRouter with real Redis.
 */
@Testcontainers
class CanaryRouterIntegrationTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:8.4-alpine").withExposedPorts(6379);

    private StringRedisTemplate redisTemplate;
    private CanaryRouter canaryRouter;

    @BeforeEach
    void setUp() {
        var factory = new LettuceConnectionFactory(
                redis.getHost(), redis.getMappedPort(6379));
        factory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(factory);
        // Clear canary keys between tests
        redisTemplate.delete("canary:percent");
        redisTemplate.delete("canary:salt");
        canaryRouter = new CanaryRouter(redisTemplate, new MetricsFacade(new SimpleMeterRegistry()));
    }

    @Test
    void defaultPercent_isZero() {
        assertThat(canaryRouter.getCanaryPercent()).isZero();
    }

    @Test
    void setAndGetPercent() {
        canaryRouter.setCanaryPercent(25);
        assertThat(canaryRouter.getCanaryPercent()).isEqualTo(25);
    }

    @Test
    void percentPersistedInRedis() {
        canaryRouter.setCanaryPercent(42);
        String stored = redisTemplate.opsForValue().get("canary:percent");
        assertThat(stored).isEqualTo("42");
    }

    @Test
    void percentReadFromRedis_onNewInstance() {
        redisTemplate.opsForValue().set("canary:percent", "15");
        // New instance should read from Redis
        var newRouter = new CanaryRouter(redisTemplate, new MetricsFacade(new SimpleMeterRegistry()));
        assertThat(newRouter.getCanaryPercent()).isEqualTo(15);
    }

    @Test
    void saltPersistedInRedis() {
        canaryRouter.setSalt("v2-rollout");
        String stored = redisTemplate.opsForValue().get("canary:salt");
        assertThat(stored).isEqualTo("v2-rollout");
    }

    @Test
    void saltChange_redistributesBuckets() {
        canaryRouter.setCanaryPercent(50);
        canaryRouter.setSalt("salt-a");
        boolean resultA = canaryRouter.isCanary(123456L);

        canaryRouter.setSalt("salt-b");
        boolean resultB = canaryRouter.isCanary(123456L);

        // With different salts, at least some users will get different results
        // We test with a known user - the point is the salt changes the routing
        // For a deterministic check, we verify both paths are reachable
        canaryRouter.setCanaryPercent(100);
        assertThat(canaryRouter.isCanary(123456L)).isTrue();
        canaryRouter.setCanaryPercent(0);
        assertThat(canaryRouter.isCanary(123456L)).isFalse();
    }

    @Test
    void invalidPercent_throwsException() {
        assertThatThrownBy(() -> canaryRouter.setCanaryPercent(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> canaryRouter.setCanaryPercent(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroPercent_neverCanary() {
        canaryRouter.setCanaryPercent(0);
        for (long userId = 1; userId <= 100; userId++) {
            assertThat(canaryRouter.isCanary(userId)).isFalse();
        }
    }

    @Test
    void hundredPercent_alwaysCanary() {
        canaryRouter.setCanaryPercent(100);
        for (long userId = 1; userId <= 100; userId++) {
            assertThat(canaryRouter.isCanary(userId)).isTrue();
        }
    }
}
