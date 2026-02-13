package com.advertmarket.marketplace.channel.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for Telegram bot identity used in channel verification.
 *
 * @param botUserId the Telegram user ID of the bot
 */
@ConfigurationProperties(prefix = "app.marketplace.channel")
@PropertyGroupDoc(
        displayName = "Channel Bot",
        description = "Bot identity for channel admin verification",
        category = "Marketplace"
)
@Validated
public record ChannelBotProperties(
        @PropertyDoc(
                description = "Telegram user ID of the bot",
                required = Requirement.REQUIRED
        )
        @Positive long botUserId
) {
}
