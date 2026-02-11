package com.advertmarket.communication.bot.internal.sender;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Telegram message sender.
 *
 * @param globalPerSec          max messages per second globally
 * @param perChatPerSec         max messages per second per chat
 * @param cacheExpireAfterAccess per-chat semaphore cache expiry
 * @param cacheMaximumSize      max per-chat semaphore cache entries
 * @param replenishFixedRateMs  semaphore replenish interval (ms)
 */
@ConfigurationProperties(prefix = "app.telegram.sender")
@Validated
public record TelegramSenderProperties(
        @PropertyDoc(description = "Global rate limit messages/sec")
        @Positive @DefaultValue("30") int globalPerSec,
        @PropertyDoc(description = "Per-chat rate limit messages/sec")
        @Positive @DefaultValue("1") int perChatPerSec,
        @DefaultValue("5m") Duration cacheExpireAfterAccess,
        @Positive @DefaultValue("10000") int cacheMaximumSize,
        @Positive @DefaultValue("1000") long replenishFixedRateMs
) {
}
