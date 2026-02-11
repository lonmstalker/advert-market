package com.advertmarket.communication.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@DisplayName("UpdateDeduplicator")
class UpdateDeduplicatorTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private UpdateDeduplicator deduplicator;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        var props = new DeduplicationProperties(
                Duration.ofHours(24));
        deduplicator = new UpdateDeduplicator(
                redis, props, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("Returns true for new update ID")
    void tryAcquire_returnsTrueForNew() {
        when(valueOps.setIfAbsent(anyString(), eq("1"),
                any(Duration.class))).thenReturn(true);

        assertThat(deduplicator.tryAcquire(100)).isTrue();
    }

    @Test
    @DisplayName("Returns false for duplicate update ID")
    void tryAcquire_returnsFalseForDuplicate() {
        when(valueOps.setIfAbsent(anyString(), eq("1"),
                any(Duration.class))).thenReturn(false);

        assertThat(deduplicator.tryAcquire(100)).isFalse();
    }

    @Test
    @DisplayName("Writes update ID to Redis with TTL")
    void tryAcquire_writesToRedisWithTtl() {
        when(valueOps.setIfAbsent(anyString(), eq("1"),
                any(Duration.class))).thenReturn(true);

        deduplicator.tryAcquire(42);

        verify(valueOps).setIfAbsent(
                "tg:update:42", "1", Duration.ofHours(24));
    }

    @Test
    @DisplayName("Increments acquired counter on success")
    void tryAcquire_incrementsCounter() {
        var registry = new SimpleMeterRegistry();
        var props = new DeduplicationProperties(
                Duration.ofHours(24));
        var dedup = new UpdateDeduplicator(
                redis, props, registry);
        when(valueOps.setIfAbsent(anyString(), eq("1"),
                any(Duration.class))).thenReturn(true);

        dedup.tryAcquire(1);

        var counter = registry
                .find("telegram.update.dedup.acquired")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
