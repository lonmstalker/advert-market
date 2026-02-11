package com.advertmarket.communication.bot.internal.resilience;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyExample;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
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
@PropertyGroupDoc(
        displayName = "Telegram Resilience",
        description = "Circuit breaker and bulkhead settings for Telegram API",
        category = "Telegram"
)
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
            @PropertyDoc(
                    description = "Count-based sliding window size",
                    required = Requirement.OPTIONAL
            )
            @Positive @DefaultValue("50") int slidingWindowSize,

            @PropertyDoc(
                    description = "Failure rate percentage to open circuit",
                    required = Requirement.OPTIONAL
            )
            @PropertyExample("40")
            @PropertyExample("60")
            @Positive @DefaultValue("40") int failureRateThreshold,

            @PropertyDoc(
                    description = "Threshold duration for slow calls",
                    required = Requirement.OPTIONAL
            )
            @DefaultValue("5s") Duration slowCallDuration,

            @PropertyDoc(
                    description = "Wait duration in open state before half-open",
                    required = Requirement.OPTIONAL
            )
            @DefaultValue("15s") Duration waitInOpenState,

            @PropertyDoc(
                    description = "Permitted calls in half-open state",
                    required = Requirement.OPTIONAL
            )
            @Positive @DefaultValue("10") int halfOpenCalls,

            @PropertyDoc(
                    description = "Minimum calls before evaluating failure rate",
                    required = Requirement.OPTIONAL
            )
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
            @PropertyDoc(
                    description = "Max concurrent Telegram API calls",
                    required = Requirement.OPTIONAL
            )
            @Positive @DefaultValue("30") int maxConcurrentCalls,

            @PropertyDoc(
                    description = "Max wait duration for a bulkhead permit",
                    required = Requirement.OPTIONAL
            )
            @DefaultValue("1s") Duration maxWaitDuration
    ) {
    }
}
