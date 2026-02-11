package com.advertmarket.communication.bot.internal.error;

import static com.advertmarket.communication.bot.internal.BotConstants.METRIC_WEBHOOK_ERROR;

import com.advertmarket.shared.metric.MetricsFacade;
import com.fasterxml.jackson.core.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the webhook endpoint.
 *
 * <p>Always returns 200 OK because Telegram retries on non-200
 * responses. Errors are logged and counted via metrics.
 */
@Slf4j
@ControllerAdvice(
        basePackages = "com.advertmarket.communication.webhook")
public class BotExceptionHandler {

    private final MetricsFacade metrics;

    /** Creates the exception handler with metrics support. */
    public BotExceptionHandler(MetricsFacade metrics) {
        this.metrics = metrics;
    }

    /** Handles JSON parse errors from malformed payloads. */
    @ExceptionHandler(JsonParseException.class)
    public ResponseEntity<Void> handleJsonParse(
            JsonParseException exception) {
        metrics.incrementCounter(METRIC_WEBHOOK_ERROR,
                "type", "json_parse");
        log.warn("Malformed webhook payload", exception);
        return ResponseEntity.badRequest().build();
    }

    /** Handles all other exceptions, 200 to prevent retries. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleGeneral(
            Exception exception) {
        metrics.incrementCounter(METRIC_WEBHOOK_ERROR,
                "type", "general");
        log.error("Webhook processing error", exception);
        return ResponseEntity.ok().build();
    }
}
