package com.advertmarket.financial.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for TON Center API resilience patterns.
 *
 * @param circuitBreaker circuit breaker settings
 * @param bulkhead       bulkhead settings
 */
@ConfigurationProperties(prefix = "app.ton.resilience")
@PropertyGroupDoc(
        displayName = "TON Resilience",
        description = "Circuit breaker and bulkhead settings for TON Center API",
        category = "Financial"
)
@Validated
public record TonResilienceProperties(
        @PropertyDoc(
                description = "Circuit breaker settings",
                required = Requirement.OPTIONAL
        )
        @Valid @DefaultValue CircuitBreaker circuitBreaker,

        @PropertyDoc(
                description = "Bulkhead settings for concurrent call limiting",
                required = Requirement.OPTIONAL
        )
        @Valid @DefaultValue Bulkhead bulkhead
) {

    /**
     * Circuit breaker settings for TON Center API.
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
            @Positive @DefaultValue("20") int slidingWindowSize,

            @PropertyDoc(
                    description = "Failure rate percentage to open circuit",
                    required = Requirement.OPTIONAL
            )
            @Positive @DefaultValue("50") int failureRateThreshold,

            @PropertyDoc(
                    description = "Threshold duration for slow calls",
                    required = Requirement.OPTIONAL
            )
            @DefaultValue("10s") Duration slowCallDuration,

            @PropertyDoc(
                    description = "Wait duration in open state before half-open",
                    required = Requirement.OPTIONAL
            )
            @DefaultValue("30s") Duration waitInOpenState,

            @PropertyDoc(
                    description = "Permitted calls in half-open state",
                    required = Requirement.OPTIONAL
            )
            @Positive @DefaultValue("5") int halfOpenCalls,

            @PropertyDoc(
                    description = "Minimum calls before evaluating failure rate",
                    required = Requirement.OPTIONAL
            )
            @Positive @DefaultValue("10") int minimumCalls
    ) {
    }

    /**
     * Bulkhead settings for concurrent TON Center API calls.
     *
     * @param maxConcurrentCalls max concurrent API calls
     * @param maxWaitDuration    max wait for a permit
     */
    public record Bulkhead(
            @PropertyDoc(
                    description = "Max concurrent TON Center API calls",
                    required = Requirement.OPTIONAL
            )
            @Positive @DefaultValue("10") int maxConcurrentCalls,

            @PropertyDoc(
                    description = "Max wait duration for a bulkhead permit",
                    required = Requirement.OPTIONAL
            )
            @DefaultValue("2s") Duration maxWaitDuration
    ) {
    }
}