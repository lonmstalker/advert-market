package com.advertmarket.communication.bot.internal.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.metric.MetricsFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class BotErrorHandlerTest {

    private final SimpleMeterRegistry registry =
            new SimpleMeterRegistry();
    private final MetricsFacade metrics = new MetricsFacade(registry);
    private final BotErrorHandler handler = new BotErrorHandler(metrics);

    @Test
    void handle_incrementsMetric() {
        handler.handle(new RuntimeException("test"), 1);

        var counter = registry.find("telegram.handler.error")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void classify_rateLimited() {
        assertThat(BotErrorHandler.classify(
                new RuntimeException("rate limit exceeded")))
                .isEqualTo(BotErrorCategory.RATE_LIMITED);
    }

    @Test
    void classify_userBlocked() {
        assertThat(BotErrorHandler.classify(
                new RuntimeException("Forbidden: bot was blocked")))
                .isEqualTo(BotErrorCategory.USER_BLOCKED);
    }

    @Test
    void classify_clientError() {
        assertThat(BotErrorHandler.classify(
                new RuntimeException("Bad Request: wrong type")))
                .isEqualTo(BotErrorCategory.CLIENT_ERROR);
    }

    @Test
    void classify_unknown() {
        assertThat(BotErrorHandler.classify(
                new RuntimeException("something unexpected")))
                .isEqualTo(BotErrorCategory.UNKNOWN);
    }

    @Test
    void classify_nullMessage() {
        assertThat(BotErrorHandler.classify(
                new RuntimeException((String) null)))
                .isEqualTo(BotErrorCategory.UNKNOWN);
    }
}
