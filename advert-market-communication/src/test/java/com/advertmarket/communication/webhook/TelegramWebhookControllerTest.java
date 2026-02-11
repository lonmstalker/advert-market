package com.advertmarket.communication.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.bot.internal.config.TelegramBotProperties;
import com.advertmarket.communication.bot.internal.config.TelegramBotProperties.Webhook;
import com.pengrad.telegrambot.model.Update;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TelegramWebhookController")
class TelegramWebhookControllerTest {

    private UpdateDeduplicationPort deduplicator;
    private UpdateProcessor processor;
    private TelegramWebhookController controller;

    @BeforeEach
    void setUp() {
        deduplicator = mock(UpdateDeduplicationPort.class);
        processor = mock(UpdateProcessor.class);
        var botProps = mock(TelegramBotProperties.class);
        var webhook = new Webhook("", "my-secret");
        when(botProps.webhook()).thenReturn(webhook);
        controller = new TelegramWebhookController(
                botProps, deduplicator, processor,
                new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("Returns 200 for valid update with correct secret")
    void handleWebhook_validUpdate_returns200() {
        when(deduplicator.tryAcquire(anyInt())).thenReturn(true);
        String body = "{\"update_id\": 123}";

        var response = controller.handleWebhook(
                "my-secret", body);

        assertThat(response.getStatusCode().value())
                .isEqualTo(200);
        verify(processor).processAsync(
                org.mockito.ArgumentMatchers.any(Update.class));
    }

    @Test
    @DisplayName("Returns 401 for invalid secret")
    void handleWebhook_invalidSecret_returns401() {
        String body = "{\"update_id\": 123}";

        var response = controller.handleWebhook(
                "wrong-secret", body);

        assertThat(response.getStatusCode().value())
                .isEqualTo(401);
        verify(processor, never()).processAsync(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Duplicate update returns 200 without processing")
    void handleWebhook_duplicate_returns200() {
        when(deduplicator.tryAcquire(123)).thenReturn(false);
        String body = "{\"update_id\": 123}";

        var response = controller.handleWebhook(
                "my-secret", body);

        assertThat(response.getStatusCode().value())
                .isEqualTo(200);
        verify(processor, never()).processAsync(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Accepts request when webhook secret is empty")
    void handleWebhook_emptySecret_acceptsAll() {
        var botProps = mock(TelegramBotProperties.class);
        var webhook = new Webhook("", "");
        when(botProps.webhook()).thenReturn(webhook);
        var ctrl = new TelegramWebhookController(
                botProps, deduplicator, processor,
                new SimpleMeterRegistry());
        when(deduplicator.tryAcquire(anyInt())).thenReturn(true);

        var response = ctrl.handleWebhook(
                null, "{\"update_id\": 1}");

        assertThat(response.getStatusCode().value())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Returns 400 for malformed JSON body")
    void handleWebhook_malformedBody_returns400() {
        var response = controller.handleWebhook(
                "my-secret", "not json");

        assertThat(response.getStatusCode().value())
                .isEqualTo(400);
    }
}
