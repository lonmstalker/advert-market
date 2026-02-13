package com.advertmarket.communication.bot.internal.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.metric.MetricsFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BotErrorHandler")
class BotErrorHandlerTest {

    private final SimpleMeterRegistry registry =
            new SimpleMeterRegistry();
    private final MetricsFacade metrics = new MetricsFacade(registry);
    private final TelegramSender sender =
            mock(TelegramSender.class);
    private final LocalizationService i18n =
            mock(LocalizationService.class);
    private final BotErrorHandler handler =
            new BotErrorHandler(metrics, sender, i18n);

    @Test
    @DisplayName("Increments error metric counter")
    void handle_incrementsMetric() {
        handler.handle(new RuntimeException("test"), 1);

        var counter = registry.find("telegram.handler.error")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Classifies rate limit error")
    void classify_rateLimited() {
        assertThat(BotErrorHandler.classify(
                new RuntimeException("rate limit exceeded")))
                .isEqualTo(BotErrorCategory.RATE_LIMITED);
    }

    @Test
    @DisplayName("Classifies user blocked error")
    void classify_userBlocked() {
        assertThat(BotErrorHandler.classify(
                new RuntimeException("Forbidden: bot was blocked")))
                .isEqualTo(BotErrorCategory.USER_BLOCKED);
    }

    @Test
    @DisplayName("Classifies client error")
    void classify_clientError() {
        assertThat(BotErrorHandler.classify(
                new RuntimeException("Bad Request: wrong type")))
                .isEqualTo(BotErrorCategory.CLIENT_ERROR);
    }

    @Test
    @DisplayName("Classifies unknown error")
    void classify_unknown() {
        assertThat(BotErrorHandler.classify(
                new RuntimeException("something unexpected")))
                .isEqualTo(BotErrorCategory.UNKNOWN);
    }

    @Test
    @DisplayName("Classifies null message as unknown")
    void classify_nullMessage() {
        assertThat(BotErrorHandler.classify(
                new RuntimeException((String) null)))
                .isEqualTo(BotErrorCategory.UNKNOWN);
    }

    @Test
    @DisplayName("handleAndNotify sends i18n error message")
    void handleAndNotify_sendsI18nMessage() {
        when(i18n.msg("bot.error", "en"))
                .thenReturn("An error occurred");

        handler.handleAndNotify(
                new RuntimeException("test"), 1, 100L, "en");

        verify(sender).send(100L, "An error occurred");
    }

    @Test
    @DisplayName("handleAndNotify passes null lang to i18n")
    void handleAndNotify_nullLangDelegatesToI18n() {
        when(i18n.msg("bot.error", (String) null))
                .thenReturn("Произошла ошибка");

        handler.handleAndNotify(
                new RuntimeException("test"), 1, 100L, null);

        verify(sender).send(100L, "Произошла ошибка");
    }

    @Test
    @DisplayName("handleAndNotify swallows sender exception")
    void handleAndNotify_swallowsSenderException() {
        when(i18n.msg("bot.error", "ru"))
                .thenReturn("error msg");
        doThrow(new RuntimeException("send failed"))
                .when(sender).send(anyLong(), anyString());

        handler.handleAndNotify(
                new RuntimeException("test"), 1, 100L, "ru");

        var counter = registry.find("telegram.handler.error")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("handleAndNotify increments metric")
    void handleAndNotify_incrementsMetric() {
        when(i18n.msg("bot.error", "ru"))
                .thenReturn("error msg");

        handler.handleAndNotify(
                new RuntimeException("rate limit"), 42,
                100L, "ru");

        var counter = registry.find("telegram.handler.error")
                .tag("category", "RATE_LIMITED")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
