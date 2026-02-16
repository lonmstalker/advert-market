package com.advertmarket.communication.webhook;

import com.advertmarket.communication.bot.internal.config.TelegramBotProperties;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.utility.BotUtils;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Handles Telegram webhook updates: secret validation, parsing, dedup, async dispatch.
 *
 * <p>Extracted from the controller to keep {@code ..web..} thin.
 */
@Slf4j
@Component
public class TelegramWebhookHandler {

    private static final String REJECT_REASON = "reason";
    private static final String REASON_INVALID_SECRET = "invalid_secret";
    private static final String REASON_PARSE_ERROR = "parse_error";
    private static final String REASON_OVERSIZE = "oversize";
    private static final String REASON_DUPLICATE = "duplicate";

    private final String webhookSecret;
    private final int maxBodyBytes;
    private final UpdateDeduplicationPort deduplicator;
    private final UpdateProcessor processor;
    private final MetricsFacade metrics;

    /**
     * Creates a webhook handler that validates secrets and dispatches updates.
     */
    public TelegramWebhookHandler(
            TelegramBotProperties botProperties,
            UpdateDeduplicationPort deduplicator,
            UpdateProcessor processor,
            MetricsFacade metrics) {
        this.webhookSecret = botProperties.webhook().secret();
        this.maxBodyBytes = botProperties.webhook().maxBodyBytes();
        this.deduplicator = deduplicator;
        this.processor = processor;
        this.metrics = metrics;
    }

    /**
     * Handles webhook request and returns HTTP response.
     */
    public ResponseEntity<Void> handle(
            String secretToken,
            String body) {
        return metrics.recordTimer(MetricNames.WEBHOOK_LATENCY,
                () -> processWebhook(secretToken, body));
    }

    private ResponseEntity<Void> processWebhook(
            String secretToken,
            String body) {
        // 1. Validate secret
        if (!webhookSecret.equals(secretToken)) {
            log.warn("Webhook secret mismatch");
            incrementRejected(REASON_INVALID_SECRET);
            return ResponseEntity.status(
                    HttpStatus.UNAUTHORIZED).<Void>build();
        }

        // 2. Enforce payload size guardrail
        int bodyBytes = body == null
                ? 0
                : body.getBytes(StandardCharsets.UTF_8).length;
        if (bodyBytes > maxBodyBytes) {
            log.warn("Webhook payload too large: {} bytes > {}",
                    bodyBytes, maxBodyBytes);
            incrementRejected(REASON_OVERSIZE);
            return ResponseEntity.status(
                    HttpStatus.PAYLOAD_TOO_LARGE).<Void>build();
        }

        // 2. Parse update
        Update update;
        try {
            update = BotUtils.parseUpdate(body);
        } catch (Exception e) {
            log.error("Failed to parse Telegram update", e);
            incrementRejected(REASON_PARSE_ERROR);
            return ResponseEntity
                    .badRequest().<Void>build();
        }

        // 3. Deduplicate by update_id
        int updateId = update.updateId();
        if (!deduplicator.tryAcquire(updateId)) {
            metrics.incrementCounter(
                    MetricNames.DEDUP_DUPLICATE);
            incrementRejected(REASON_DUPLICATE);
            log.debug("Duplicate update_id={}, skipping",
                    updateId);
            return ResponseEntity.ok().<Void>build();
        }

        // 4. Fast ack - process async
        processor.processAsync(update);
        metrics.incrementCounter(
                MetricNames.WEBHOOK_ACCEPTED_DISPATCHED);

        return ResponseEntity.ok().<Void>build();
    }

    private void incrementRejected(String reason) {
        metrics.incrementCounter(
                MetricNames.WEBHOOK_REJECTED_TOTAL,
                REJECT_REASON, reason);
    }
}
