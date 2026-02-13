package com.advertmarket.communication.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.bot.internal.config.TelegramBotProperties;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SetWebhook;
import com.pengrad.telegrambot.response.BaseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("TelegramWebhookRegistrar")
class TelegramWebhookRegistrarTest {

    @Test
    @DisplayName("Skips registration when webhook URL is blank")
    void registerWebhook_skipsWhenUrlBlank() {
        TelegramBot bot = mock(TelegramBot.class);

        var props = mock(TelegramBotProperties.class);
        when(props.webhook()).thenReturn(new TelegramBotProperties.Webhook("", "secret"));

        var registrar = new TelegramWebhookRegistrar(bot, props);
        registrar.registerWebhook();

        verify(bot, never()).execute(any());
    }

    @Test
    @DisplayName("Calls Telegram setWebhook with URL + secret token")
    void registerWebhook_callsSetWebhook() {
        TelegramBot bot = mock(TelegramBot.class);

        var props = mock(TelegramBotProperties.class);
        when(props.webhook()).thenReturn(
                new TelegramBotProperties.Webhook("https://example.com/api/v1/bot/webhook", "secret-token")
        );

        BaseResponse ok = mock(BaseResponse.class);
        when(ok.isOk()).thenReturn(true);
        when(bot.execute(any(SetWebhook.class))).thenReturn(ok);

        var registrar = new TelegramWebhookRegistrar(bot, props);
        registrar.registerWebhook();

        var captor = ArgumentCaptor.forClass(SetWebhook.class);
        verify(bot).execute(captor.capture());

        SetWebhook request = captor.getValue();
        assertThat(request.getParameters()).containsEntry("url", "https://example.com/api/v1/bot/webhook");
        assertThat(request.getParameters()).containsEntry("secret_token", "secret-token");
        assertThat(request.getParameters()).containsKey("allowed_updates");

        Object allowedUpdatesRaw = request.getParameters().get("allowed_updates");
        assertThat(allowedUpdatesRaw).isInstanceOf(String[].class);
        assertThat((String[]) allowedUpdatesRaw).containsExactly("message", "callback_query");
    }

    @Test
    @DisplayName("Fails fast when Telegram rejects setWebhook")
    void registerWebhook_failsFastWhenRejected() {
        TelegramBot bot = mock(TelegramBot.class);

        var props = mock(TelegramBotProperties.class);
        when(props.webhook()).thenReturn(
                new TelegramBotProperties.Webhook("https://example.com/api/v1/bot/webhook", "secret-token")
        );

        BaseResponse rejected = mock(BaseResponse.class);
        when(rejected.isOk()).thenReturn(false);
        when(rejected.errorCode()).thenReturn(401);
        when(rejected.description()).thenReturn("Unauthorized");
        when(bot.execute(any(SetWebhook.class))).thenReturn(rejected);

        var registrar = new TelegramWebhookRegistrar(bot, props);

        assertThatThrownBy(registrar::registerWebhook)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("setWebhook failed");
    }
}

