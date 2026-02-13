package com.advertmarket.communication.webhook;

import com.advertmarket.communication.bot.internal.config.TelegramBotProperties;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.utility.BotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Telegram webhook endpoint.
 * Validates secret token, deduplicates by update_id,
 * acks fast (200 OK), processes async.
 */
@Slf4j
@RestController
public class TelegramWebhookController {

    private final String webhookSecret;
    private final UpdateDeduplicationPort deduplicator;
    private final UpdateProcessor processor;
    private final MetricsFacade metrics;

    /** Creates the webhook controller. */
    public TelegramWebhookController(
            TelegramBotProperties botProperties,
            UpdateDeduplicationPort deduplicator,
            UpdateProcessor processor,
            MetricsFacade metrics) {
        this.webhookSecret = botProperties.webhook().secret();
        this.deduplicator = deduplicator;
        this.processor = processor;
        this.metrics = metrics;
    }

    /** Handles incoming Telegram webhook updates. */
    @PostMapping("/api/v1/bot/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(
                    value = "X-Telegram-Bot-Api-Secret-Token",
                    required = false) String secretToken,
            @RequestBody String body) {

        return metrics.recordTimer(MetricNames.WEBHOOK_LATENCY,
                () -> processWebhook(secretToken, body));
    }

    private ResponseEntity<Void> processWebhook(
            String secretToken, String body) {
        // 1. Validate secret
        if (!webhookSecret.equals(secretToken)) {
            log.warn("Webhook secret mismatch");
            return ResponseEntity.status(
                    HttpStatus.UNAUTHORIZED).<Void>build();
        }

        // 2. Parse update
        Update update;
        try {
            update = BotUtils.parseUpdate(body);
        } catch (Exception e) {
            log.error("Failed to parse Telegram update", e);
            return ResponseEntity
                    .badRequest().<Void>build();
        }

        // 3. Deduplicate by update_id
        int updateId = update.updateId();
        if (!deduplicator.tryAcquire(updateId)) {
            metrics.incrementCounter(
                    MetricNames.DEDUP_DUPLICATE);
            log.debug("Duplicate update_id={}, skipping",
                    updateId);
            return ResponseEntity.ok().<Void>build();
        }

        // 4. Fast ack - process async
        processor.processAsync(update);

        return ResponseEntity.ok().<Void>build();
    }
}
