package com.advertmarket.integration.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.lock.RedisDistributedLock;
import com.advertmarket.shared.metric.MetricsFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for RedisDistributedLock with real Redis.
 */
@Testcontainers
@DisplayName("RedisDistributedLock — Redis integration")
class RedisDistributedLockIntegrationTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:8.4-alpine")
                    .withExposedPorts(6379);

    private RedisDistributedLock lock;

    @BeforeEach
    void setUp() {
        var factory = new LettuceConnectionFactory(
                redis.getHost(), redis.getMappedPort(6379));
        factory.afterPropertiesSet();
        var redisTemplate = new StringRedisTemplate(factory);
        var metrics = new MetricsFacade(new SimpleMeterRegistry());
        lock = new RedisDistributedLock(redisTemplate, metrics);
    }

    @Test
    @DisplayName("Acquire and release lock successfully")
    void acquireAndRelease() {
        Optional<String> token =
                lock.tryLock("test:1", Duration.ofSeconds(10));

        assertThat(token).isPresent();

        lock.unlock("test:1", token.get());

        // Should be able to acquire again after release
        Optional<String> secondToken =
                lock.tryLock("test:1", Duration.ofSeconds(10));
        assertThat(secondToken).isPresent();
        lock.unlock("test:1", secondToken.get());
    }

    @Test
    @DisplayName("Second acquire fails while lock is held")
    void reentrance_fails() {
        Optional<String> token =
                lock.tryLock("test:2", Duration.ofSeconds(10));
        assertThat(token).isPresent();

        Optional<String> secondAttempt =
                lock.tryLock("test:2", Duration.ofSeconds(10));
        assertThat(secondAttempt).isEmpty();

        lock.unlock("test:2", token.get());
    }

    @Test
    @DisplayName("Lock expires after TTL")
    void ttlExpiry() throws Exception {
        Optional<String> token =
                lock.tryLock("test:3", Duration.ofSeconds(1));
        assertThat(token).isPresent();

        Thread.sleep(1500);

        // Lock should have expired
        Optional<String> newToken =
                lock.tryLock("test:3", Duration.ofSeconds(10));
        assertThat(newToken).isPresent();
        lock.unlock("test:3", newToken.get());
    }

    @Test
    @DisplayName("Unlock with wrong token does not release lock")
    void unlock_wrongToken_doesNotRelease() {
        Optional<String> token =
                lock.tryLock("test:4", Duration.ofSeconds(10));
        assertThat(token).isPresent();

        lock.unlock("test:4", "wrong-token");

        // Lock should still be held
        Optional<String> secondAttempt =
                lock.tryLock("test:4", Duration.ofSeconds(10));
        assertThat(secondAttempt).isEmpty();

        lock.unlock("test:4", token.get());
    }

    @Test
    @DisplayName("withLock executes action and releases lock")
    void withLock_executesAndReleases() {
        String result = lock.withLock(
                "test:5", Duration.ofSeconds(10),
                () -> "done");

        assertThat(result).isEqualTo("done");

        // Lock should be released
        Optional<String> token =
                lock.tryLock("test:5", Duration.ofSeconds(10));
        assertThat(token).isPresent();
        lock.unlock("test:5", token.get());
    }

    @Test
    @DisplayName("Different keys do not interfere")
    void differentKeys_noInterference() {
        Optional<String> tokenA =
                lock.tryLock("test:a", Duration.ofSeconds(10));
        Optional<String> tokenB =
                lock.tryLock("test:b", Duration.ofSeconds(10));

        assertThat(tokenA).isPresent();
        assertThat(tokenB).isPresent();

        lock.unlock("test:a", tokenA.get());
        lock.unlock("test:b", tokenB.get());
    }
}
