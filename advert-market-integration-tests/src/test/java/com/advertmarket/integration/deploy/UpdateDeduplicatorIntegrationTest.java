package com.advertmarket.integration.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.communication.webhook.UpdateDeduplicator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for UpdateDeduplicator with real Redis.
 */
@Testcontainers
class UpdateDeduplicatorIntegrationTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:8.4-alpine").withExposedPorts(6379);

    private StringRedisTemplate redisTemplate;
    private UpdateDeduplicator deduplicator;

    @BeforeEach
    void setUp() {
        var factory = new LettuceConnectionFactory(
                redis.getHost(), redis.getMappedPort(6379));
        factory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(factory);
        deduplicator = new UpdateDeduplicator(redisTemplate, new SimpleMeterRegistry());
    }

    @Test
    void firstAcquire_returnsTrue() {
        assertThat(deduplicator.tryAcquire(100001)).isTrue();
    }

    @Test
    void secondAcquire_returnsFalse() {
        deduplicator.tryAcquire(100002);
        assertThat(deduplicator.tryAcquire(100002)).isFalse();
    }

    @Test
    void differentUpdateIds_areIndependent() {
        assertThat(deduplicator.tryAcquire(200001)).isTrue();
        assertThat(deduplicator.tryAcquire(200002)).isTrue();
    }

    @Test
    void keyIsStoredInRedis() {
        deduplicator.tryAcquire(300001);
        String value = redisTemplate.opsForValue().get("tg:update:300001");
        assertThat(value).isEqualTo("1");
    }

    @Test
    void keyHasTtl() {
        deduplicator.tryAcquire(400001);
        Long ttl = redisTemplate.getExpire("tg:update:400001");
        assertThat(ttl).isNotNull().isGreaterThan(0);
    }
}
