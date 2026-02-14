package com.advertmarket.communication.bot.internal.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyExample;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
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
 * @param welcome     welcome message configuration
 */
@ConfigurationProperties(prefix = "app.telegram")
@PropertyGroupDoc(
        displayName = "Telegram Bot",
        description = "Root configuration for the Telegram bot",
        category = "Telegram"
)
@Validated
public record TelegramBotProperties(
        @PropertyDoc(
                description = "Telegram bot token from BotFather",
                required = Requirement.REQUIRED,
                sensitive = true
        )
        @PropertyExample("123456:ABC-DEF...")
        @NotBlank String botToken,

        @PropertyDoc(
                description = "Telegram bot username",
                required = Requirement.REQUIRED
        )
        @NotBlank String botUsername,

        @PropertyDoc(
                description = "Webhook configuration",
                required = Requirement.OPTIONAL
        )
        @Valid @DefaultValue Webhook webhook,

        @PropertyDoc(
                description = "Telegram Web App configuration",
                required = Requirement.REQUIRED
        )
        @Valid WebApp webapp,

        @PropertyDoc(
                description = "Welcome message configuration",
                required = Requirement.OPTIONAL
        )
        @Valid @DefaultValue Welcome welcome
) {

    /**
     * Webhook configuration.
     *
     * @param url    the webhook URL
     * @param secret the secret token for webhook validation
     */
    public record Webhook(
            @PropertyDoc(
                    description = "Webhook URL for receiving updates",
                    required = Requirement.OPTIONAL
            )
            @DefaultValue("") String url,

            @PropertyDoc(
                    description = "Secret token for webhook validation",
                    required = Requirement.OPTIONAL,
                    sensitive = true
            )
            @NotBlank String secret
    ) {
    }

    /**
     * Telegram Web App configuration.
     *
     * @param url the web app URL
     */
    public record WebApp(
            @PropertyDoc(
                    description = "Telegram Web App URL",
                    required = Requirement.REQUIRED
            )
            @NotBlank String url
    ) {
    }

    /**
     * Welcome message configuration.
     *
     * @param customEmojiId custom emoji id to prefix the welcome message
     */
    public record Welcome(
            @PropertyDoc(
                    description = "Custom emoji id to prefix the welcome message (MarkdownV2: ![x](tg://emoji?id=...))",
                    required = Requirement.OPTIONAL
            )
            @DefaultValue("") String customEmojiId
    ) {
    }
}
