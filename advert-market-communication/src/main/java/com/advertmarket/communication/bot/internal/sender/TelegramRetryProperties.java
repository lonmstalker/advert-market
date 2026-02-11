package com.advertmarket.communication.bot.internal.sender;

import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Telegram API retry behaviour.
 *
 * @param maxAttempts      max retry attempts (including initial)
 * @param backoffIntervals backoff durations between retries
 */
@ConfigurationProperties(prefix = "app.telegram.retry")
@Validated
public record TelegramRetryProperties(
        @Positive @DefaultValue("3") int maxAttempts,
        @DefaultValue({"1s", "2s", "4s"})
        List<Duration> backoffIntervals
) {
}
