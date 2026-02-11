package com.advertmarket.communication.webhook;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.utility.BotUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Telegram webhook endpoint.
 * Validates secret token, deduplicates by update_id, acks fast (200 OK), processes async.
 */
@Slf4j
@RestController
public class TelegramWebhookController {

    private final String webhookSecret;
    private final UpdateDeduplicator deduplicator;
    private final UpdateProcessor processor;
    private final Timer webhookLatency;
    private final Counter duplicatesCounter;

    /** Creates the webhook controller. */
    public TelegramWebhookController(
            @Value("${app.telegram.webhook.secret:}") String webhookSecret,
            UpdateDeduplicator deduplicator,
            UpdateProcessor processor,
            MeterRegistry meterRegistry) {
        this.webhookSecret = webhookSecret;
        this.deduplicator = deduplicator;
        this.processor = processor;
        this.webhookLatency = Timer.builder("telegram.webhook.latency")
                .description("Webhook ack latency")
                .register(meterRegistry);
        this.duplicatesCounter = Counter.builder("telegram.update.duplicates")
                .description("Duplicate update_id count")
                .register(meterRegistry);
    }

    /** Handles incoming Telegram webhook updates. */
    @PostMapping("/api/v1/bot/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token",
                    required = false) String secretToken,
            @RequestBody String body) {

        return webhookLatency.record(() -> {
            // 1. Validate secret
            if (webhookSecret != null && !webhookSecret.isEmpty()
                    && !webhookSecret.equals(secretToken)) {
                log.warn("Webhook secret mismatch");
                return ResponseEntity.status(401).<Void>build();
            }

            // 2. Parse update
            Update update;
            try {
                update = BotUtils.parseUpdate(body);
            } catch (Exception e) {
                log.error("Failed to parse Telegram update", e);
                return ResponseEntity.badRequest().<Void>build();
            }

            // 3. Deduplicate by update_id
            int updateId = update.updateId();
            if (!deduplicator.tryAcquire(updateId)) {
                duplicatesCounter.increment();
                log.debug("Duplicate update_id={}, skipping", updateId);
                return ResponseEntity.ok().<Void>build();
            }

            // 4. Fast ack - process async
            processor.processAsync(update);

            return ResponseEntity.ok().<Void>build();
        });
    }
}
