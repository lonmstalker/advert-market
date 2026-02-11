package com.advertmarket.communication.bot.internal.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pengrad.telegrambot.TelegramBot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TelegramBotConfig")
class TelegramBotConfigTest {

    private final TelegramBotConfig config =
            new TelegramBotConfig();

    @Test
    @DisplayName("Creates TelegramBot bean with token")
    void telegramBot_createsWithToken() {
        var props = mock(TelegramBotProperties.class);
        when(props.botToken()).thenReturn("test-token");

        TelegramBot bot = config.telegramBot(props);

        assertThat(bot).isNotNull();
    }

    @Test
    @DisplayName("TelegramBot bean is created from properties")
    void telegramBot_usesPropertiesToken() {
        var props = mock(TelegramBotProperties.class);
        when(props.botToken()).thenReturn("123:ABC-token");

        TelegramBot bot = config.telegramBot(props);

        assertThat(bot).isNotNull();
    }
}
