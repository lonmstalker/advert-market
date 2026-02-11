package com.advertmarket.communication.bot.internal.config;

import com.advertmarket.communication.bot.internal.resilience.TelegramResilienceProperties;
import com.advertmarket.communication.bot.internal.sender.TelegramRetryProperties;
import com.advertmarket.communication.bot.internal.sender.TelegramSenderProperties;
import com.advertmarket.communication.webhook.DeduplicationProperties;
import com.pengrad.telegrambot.TelegramBot;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Telegram bot client.
 */
@Configuration
@EnableConfigurationProperties({
        TelegramBotProperties.class,
        TelegramSenderProperties.class,
        TelegramResilienceProperties.class,
        TelegramRetryProperties.class,
        DeduplicationProperties.class
})
public class TelegramBotConfig {

    /** Creates the Telegram bot client from the configured token. */
    @Bean
    public TelegramBot telegramBot(TelegramBotProperties properties) {
        return new TelegramBot(properties.botToken());
    }
}
