package com.advertmarket.communication.bot.internal.error;

import static com.advertmarket.communication.bot.internal.BotConstants.METRIC_WEBHOOK_ERROR;

import com.advertmarket.shared.metric.MetricsFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the webhook endpoint.
 *
 * <p>Always returns 200 OK because Telegram retries on non-200
 * responses. Errors are logged and counted via metrics.
 */
@Slf4j
@RequiredArgsConstructor
@ControllerAdvice(
        basePackages = "com.advertmarket.communication.webhook")
public class BotExceptionHandler {

    private final MetricsFacade metrics;

    /** Handles unreadable HTTP messages (malformed JSON, etc.). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Void> handleUnreadableMessage(
            HttpMessageNotReadableException exception) {
        metrics.incrementCounter(METRIC_WEBHOOK_ERROR,
                "type", "json_parse");
        log.warn("Malformed webhook payload", exception);
        return ResponseEntity.badRequest().build();
    }

    /** Returns 500 for infrastructure errors so Telegram retries. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Void> handleInfrastructure(
            DataAccessException exception) {
        metrics.incrementCounter(METRIC_WEBHOOK_ERROR,
                "type", "infrastructure");
        log.error("Infrastructure error in webhook", exception);
        return ResponseEntity.status(
                HttpStatus.INTERNAL_SERVER_ERROR).build();
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
