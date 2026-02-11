package com.advertmarket.communication.bot.internal.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j configuration for Telegram Bot API calls.
 */
@Configuration
public class TelegramCircuitBreakerConfig {

    /** Name used for all Telegram resilience4j instances. */
    public static final String TELEGRAM_BOT = "telegram-bot";

    /** Creates the circuit breaker for Telegram API calls. */
    @Bean
    public CircuitBreaker telegramCircuitBreaker(
            TelegramResilienceProperties props) {
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
        return CircuitBreaker.of(TELEGRAM_BOT, config);
    }

    /** Creates the bulkhead for Telegram API calls. */
    @Bean
    public Bulkhead telegramBulkhead(
            TelegramResilienceProperties props) {
        var bh = props.bulkhead();
        var config = BulkheadConfig.custom()
                .maxConcurrentCalls(bh.maxConcurrentCalls())
                .maxWaitDuration(bh.maxWaitDuration())
                .build();
        return Bulkhead.of(TELEGRAM_BOT, config);
    }
}
