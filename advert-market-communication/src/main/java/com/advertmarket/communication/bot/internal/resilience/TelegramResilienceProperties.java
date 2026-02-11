package com.advertmarket.communication.bot.internal.resilience;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Telegram API resilience patterns.
 *
 * @param circuitBreaker circuit breaker settings
 * @param bulkhead       bulkhead settings
 */
@ConfigurationProperties(prefix = "app.telegram.resilience")
@Validated
public record TelegramResilienceProperties(
        @Valid @DefaultValue CircuitBreaker circuitBreaker,
        @Valid @DefaultValue Bulkhead bulkhead
) {

    /**
     * Circuit breaker settings.
     *
     * @param slidingWindowSize    count-based sliding window size
     * @param failureRateThreshold failure rate percentage to open
     * @param slowCallDuration     threshold for slow calls
     * @param waitInOpenState      wait duration in open state
     * @param halfOpenCalls        permitted calls in half-open
     * @param minimumCalls         minimum calls before evaluating
     */
    public record CircuitBreaker(
            @Positive @DefaultValue("50") int slidingWindowSize,
            @Positive @DefaultValue("40") int failureRateThreshold,
            @DefaultValue("5s") Duration slowCallDuration,
            @DefaultValue("15s") Duration waitInOpenState,
            @Positive @DefaultValue("10") int halfOpenCalls,
            @Positive @DefaultValue("20") int minimumCalls
    ) {
    }

    /**
     * Bulkhead settings for concurrent call limiting.
     *
     * @param maxConcurrentCalls max concurrent Telegram API calls
     * @param maxWaitDuration    max wait for a permit
     */
    public record Bulkhead(
            @Positive @DefaultValue("30") int maxConcurrentCalls,
            @DefaultValue("1s") Duration maxWaitDuration
    ) {
    }
}
