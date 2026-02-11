package com.advertmarket.communication.bot.internal.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Root configuration properties for the Telegram bot.
 *
 * @param botToken    the Telegram bot token from BotFather
 * @param botUsername the Telegram bot username
 * @param webhook     webhook configuration
 * @param webapp      web app configuration
 */
@ConfigurationProperties(prefix = "app.telegram")
@Validated
public record TelegramBotProperties(
        @PropertyDoc(description = "Telegram bot token from BotFather")
        @NotBlank String botToken,
        @PropertyDoc(description = "Telegram bot username")
        @NotBlank String botUsername,
        @Valid @DefaultValue Webhook webhook,
        @Valid WebApp webapp
) {

    /**
     * Webhook configuration.
     *
     * @param url    the webhook URL
     * @param secret the secret token for webhook validation
     */
    public record Webhook(
            @DefaultValue("") String url,
            @DefaultValue("") String secret
    ) {
    }

    /**
     * Telegram Web App configuration.
     *
     * @param url the web app URL
     */
    public record WebApp(
            @PropertyDoc(description = "Telegram Web App URL")
            @NotBlank String url
    ) {
    }
}
