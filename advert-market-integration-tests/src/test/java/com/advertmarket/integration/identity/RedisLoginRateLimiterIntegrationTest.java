package com.advertmarket.integration.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.identity.adapter.RedisLoginRateLimiter;
import com.advertmarket.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for RedisLoginRateLimiter with real Redis.
 */
@Testcontainers
@DisplayName("RedisLoginRateLimiter — Redis integration")
class RedisLoginRateLimiterIntegrationTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:8.4-alpine")
                    .withExposedPorts(6379);

    private RedisLoginRateLimiter rateLimiter;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        var factory = new LettuceConnectionFactory(
                redis.getHost(), redis.getMappedPort(6379));
        factory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(factory);
        // Clean state between tests
        redisTemplate.delete("rate:login:127.0.0.1");
        rateLimiter = new RedisLoginRateLimiter(redisTemplate);
    }

    @Test
    @DisplayName("Should allow requests under the limit")
    void shouldAllowUnderLimit() {
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkRate("127.0.0.1");
        }
    }

    @Test
    @DisplayName("Should reject requests over the limit")
    void shouldRejectOverLimit() {
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkRate("127.0.0.1");
        }

        assertThatThrownBy(
                () -> rateLimiter.checkRate("127.0.0.1"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Too many login attempts");
    }

    @Test
    @DisplayName("Should track count in Redis")
    void shouldTrackCountInRedis() {
        rateLimiter.checkRate("127.0.0.1");

        String count = redisTemplate.opsForValue()
                .get("rate:login:127.0.0.1");
        assertThat(count).isEqualTo("1");
    }
}
