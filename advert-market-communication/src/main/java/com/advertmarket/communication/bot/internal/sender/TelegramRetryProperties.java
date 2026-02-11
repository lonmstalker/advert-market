package com.advertmarket.communication.bot.internal.sender;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
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
@PropertyGroupDoc(
        displayName = "Telegram Retry",
        description = "Retry behaviour for Telegram API calls",
        category = "Telegram"
)
@Validated
public record TelegramRetryProperties(
        @PropertyDoc(
                description = "Max retry attempts including initial call",
                required = Requirement.OPTIONAL
        )
        @Positive @DefaultValue("3") int maxAttempts,

        @PropertyDoc(
                description = "Backoff durations between retries",
                required = Requirement.OPTIONAL
        )
        @DefaultValue({"1s", "2s", "4s"})
        List<Duration> backoffIntervals
) {
}
