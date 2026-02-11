package com.advertmarket.communication.bot.internal.block;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for user blocking storage.
 *
 * @param keyPrefix Redis key prefix for block entries
 */
@ConfigurationProperties(prefix = "app.telegram.block")
@PropertyGroupDoc(
        displayName = "User Blocking",
        description = "Redis-backed user blocking storage",
        category = "Telegram"
)
@Validated
public record UserBlockProperties(
        @PropertyDoc(
                description = "Redis key prefix for block entries",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("tg:block:") String keyPrefix
) {
}
