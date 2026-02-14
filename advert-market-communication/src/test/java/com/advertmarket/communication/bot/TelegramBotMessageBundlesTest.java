package com.advertmarket.communication.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Telegram bot message bundles — MarkdownV2 escaping survives .properties parsing")
class TelegramBotMessageBundlesTest {

    @Test
    @DisplayName("bot_ru.properties keeps explicit MarkdownV2 escapes (\\. and \\-)")
    void botRu_keepsMarkdownV2Escapes() {
        ResourceBundle bundle = ResourceBundle.getBundle(
                "messages.bot", Locale.of("ru"));

        assertThat(bundle.getString("bot.deeplink.opening"))
                .contains("\\.\\.\\.");
        assertThat(bundle.getString("bot.welcome"))
                .contains("Telegram\\-каналах\\.");
        assertThat(bundle.getString("bot.blocked"))
                .contains("\\.");
        assertThat(bundle.getString("bot.error"))
                .contains("\\.");
    }

    @Test
    @DisplayName("bot_en.properties keeps explicit MarkdownV2 escapes (\\.)")
    void botEn_keepsMarkdownV2Escapes() {
        ResourceBundle bundle = ResourceBundle.getBundle(
                "messages.bot", Locale.of("en"));

        assertThat(bundle.getString("bot.deeplink.opening"))
                .contains("\\.\\.\\.");
        assertThat(bundle.getString("bot.welcome"))
                .contains("\\.");
    }
}

