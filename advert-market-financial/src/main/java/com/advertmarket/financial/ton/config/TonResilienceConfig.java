package com.advertmarket.financial.ton.config;

import com.advertmarket.financial.config.TonResilienceProperties;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j configuration for TON Center API calls.
 */
@Configuration
public class TonResilienceConfig {

    /** Name used for all TON Center resilience4j instances. */
    public static final String TON_CENTER = "ton-center";

    /** Creates the circuit breaker for TON Center API calls. */
    @Bean
    public CircuitBreaker tonCenterCircuitBreaker(
            TonResilienceProperties props) {
        var cb = props.circuitBreaker();
        var config = CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(cb.slidingWindowSize())
                .failureRateThreshold(cb.failureRateThreshold())
                .slowCallDurationThreshold(cb.slowCallDuration())
                .waitDurationInOpenState(cb.waitInOpenState())
                .permittedNumberOfCallsInHalfOpenState(
                        cb.halfOpenCalls())
                .minimumNumberOfCalls(cb.minimumCalls())
                .build();
        return CircuitBreaker.of(TON_CENTER, config);
    }

    /** Creates the bulkhead for TON Center API calls. */
    @Bean
    public Bulkhead tonCenterBulkhead(
            TonResilienceProperties props) {
        var bh = props.bulkhead();
        var config = BulkheadConfig.custom()
                .maxConcurrentCalls(bh.maxConcurrentCalls())
                .maxWaitDuration(bh.maxWaitDuration())
                .build();
        return Bulkhead.of(TON_CENTER, config);
    }
}