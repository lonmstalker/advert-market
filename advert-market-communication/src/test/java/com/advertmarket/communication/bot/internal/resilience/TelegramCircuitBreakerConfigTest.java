package com.advertmarket.communication.bot.internal.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TelegramCircuitBreakerConfig")
class TelegramCircuitBreakerConfigTest {

    private final TelegramCircuitBreakerConfig config =
            new TelegramCircuitBreakerConfig();

    @Test
    @DisplayName("Creates CircuitBreaker with properties")
    void circuitBreaker_createdWithProperties() {
        var cbProps =
                new TelegramResilienceProperties.CircuitBreaker(
                        100, 50, Duration.ofSeconds(3),
                        Duration.ofSeconds(10), 5, 10);
        var bhProps =
                new TelegramResilienceProperties.Bulkhead(
                        20, Duration.ofMillis(500));
        var props = new TelegramResilienceProperties(
                cbProps, bhProps);

        CircuitBreaker cb =
                config.telegramCircuitBreaker(props);

        assertThat(cb.getName())
                .isEqualTo("telegram-bot");
        assertThat(cb.getCircuitBreakerConfig()
                .getSlidingWindowSize())
                .isEqualTo(100);
        assertThat(cb.getCircuitBreakerConfig()
                .getFailureRateThreshold())
                .isEqualTo(50);
    }

    @Test
    @DisplayName("Creates Bulkhead with properties")
    void bulkhead_createdWithProperties() {
        var cbProps =
                new TelegramResilienceProperties.CircuitBreaker(
                        50, 40, Duration.ofSeconds(5),
                        Duration.ofSeconds(15), 10, 20);
        var bhProps =
                new TelegramResilienceProperties.Bulkhead(
                        25, Duration.ofSeconds(2));
        var props = new TelegramResilienceProperties(
                cbProps, bhProps);

        Bulkhead bh = config.telegramBulkhead(props);

        assertThat(bh.getName())
                .isEqualTo("telegram-bot");
        assertThat(bh.getBulkheadConfig()
                .getMaxConcurrentCalls())
                .isEqualTo(25);
    }

    @Test
    @DisplayName("Default values produce valid configuration")
    void defaults_produceValidConfig() {
        var cbProps =
                new TelegramResilienceProperties.CircuitBreaker(
                        50, 40, Duration.ofSeconds(5),
                        Duration.ofSeconds(15), 10, 20);
        var bhProps =
                new TelegramResilienceProperties.Bulkhead(
                        30, Duration.ofSeconds(1));
        var props = new TelegramResilienceProperties(
                cbProps, bhProps);

        CircuitBreaker cb =
                config.telegramCircuitBreaker(props);
        Bulkhead bh = config.telegramBulkhead(props);

        assertThat(cb).isNotNull();
        assertThat(bh).isNotNull();
        assertThat(cb.getCircuitBreakerConfig()
                .getMinimumNumberOfCalls())
                .isEqualTo(20);
    }
}
