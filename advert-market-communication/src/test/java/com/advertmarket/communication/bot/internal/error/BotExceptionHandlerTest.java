package com.advertmarket.communication.bot.internal.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.advertmarket.shared.metric.MetricsFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BotExceptionHandler")
class BotExceptionHandlerTest {

    private final MetricsFacade metrics =
            mock(MetricsFacade.class);
    private final BotExceptionHandler handler =
            new BotExceptionHandler(metrics);

    @Test
    @DisplayName("Generic error returns 200 OK to prevent retries")
    void handleGeneral_returnsOk() {
        var exception = new RuntimeException("unexpected");
        var response = handler.handleGeneral(exception);

        assertThat(response.getStatusCode().value())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Increments error metric on generic exception")
    void handleGeneral_incrementsMetric() {
        var exception = new RuntimeException("error");
        handler.handleGeneral(exception);

        verify(metrics).incrementCounter(
                "telegram.webhook.error",
                "type", "general");
    }

    @Test
    @DisplayName("Returns ResponseEntity without body")
    void handleGeneral_returnsEmptyBody() {
        var exception = new RuntimeException("error");
        var response = handler.handleGeneral(exception);

        assertThat(response.getBody()).isNull();
    }
}
