package com.advertmarket.communication.webhook;

import lombok.RequiredArgsConstructor;
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
@RestController
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramWebhookHandler handler;

    /** Handles incoming Telegram webhook updates. */
    @PostMapping("/api/v1/bot/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(
                    value = "X-Telegram-Bot-Api-Secret-Token",
                    required = false) String secretToken,
            @RequestBody String body) {
        return handler.handle(secretToken, body);
    }
}
