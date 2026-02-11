package com.advertmarket.communication.bot.internal.sender;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TelegramRateLimiterTest {

    @Test
    void acquire_doesNotBlockWithinLimit() {
        var props = new TelegramSenderProperties(
                30, 1, Duration.ofMinutes(5), 10_000, 1000);
        var limiter = new TelegramRateLimiter(props);

        var start = Instant.now();
        limiter.acquire(1L);
        var elapsed = Duration.between(start, Instant.now());

        assertThat(elapsed).isLessThan(Duration.ofSeconds(1));
    }

    @Test
    void replenishGlobal_restoresPermits() {
        var props = new TelegramSenderProperties(
                2, 1, Duration.ofMinutes(5), 10_000, 1000);
        var limiter = new TelegramRateLimiter(props);

        // Exhaust global permits
        limiter.acquire(1L);
        limiter.acquire(2L);

        // Replenish
        limiter.replenishGlobal();
        limiter.replenishPerChat();

        // Should not block now
        var start = Instant.now();
        limiter.acquire(3L);
        var elapsed = Duration.between(start, Instant.now());
        assertThat(elapsed).isLessThan(Duration.ofSeconds(1));
    }
}
