package com.advertmarket.integration.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.communication.webhook.DeduplicationProperties;
import com.advertmarket.communication.webhook.UpdateDeduplicator;
import com.advertmarket.integration.support.RedisSupport;
import com.advertmarket.shared.metric.MetricsFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Integration test for UpdateDeduplicator with real Redis.
 */
@DisplayName("UpdateDeduplicator integration with Redis")
class UpdateDeduplicatorIntegrationTest {

    private StringRedisTemplate redisTemplate;
    private UpdateDeduplicator deduplicator;

    @BeforeEach
    void setUp() {
        redisTemplate = RedisSupport.redisTemplate();
        deduplicator = new UpdateDeduplicator(
                redisTemplate,
                new DeduplicationProperties(Duration.ofHours(24)),
                new MetricsFacade(new SimpleMeterRegistry()));
    }

    @Test
    @DisplayName("First acquire returns true")
    void firstAcquire_returnsTrue() {
        assertThat(deduplicator.tryAcquire(100001)).isTrue();
    }

    @Test
    @DisplayName("Second acquire of same update returns false")
    void secondAcquire_returnsFalse() {
        deduplicator.tryAcquire(100002);
        assertThat(deduplicator.tryAcquire(100002)).isFalse();
    }

    @Test
    @DisplayName("Different update IDs are independent")
    void differentUpdateIds_areIndependent() {
        assertThat(deduplicator.tryAcquire(200001)).isTrue();
        assertThat(deduplicator.tryAcquire(200002)).isTrue();
    }

    @Test
    @DisplayName("Key is stored in Redis")
    void keyIsStoredInRedis() {
        deduplicator.tryAcquire(300001);
        String value = redisTemplate.opsForValue().get("tg:update:300001");
        assertThat(value).isEqualTo("1");
    }

    @Test
    @DisplayName("Key has TTL set")
    void keyHasTtl() {
        deduplicator.tryAcquire(400001);
        Long ttl = redisTemplate.getExpire("tg:update:400001");
        assertThat(ttl).isNotNull().isGreaterThan(0);
    }
}
