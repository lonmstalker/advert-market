package com.advertmarket.communication.bot.internal.block;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for user blocking storage.
 *
 * @param keyPrefix Redis key prefix for block entries
 */
@ConfigurationProperties(prefix = "app.telegram.block")
@Validated
public record UserBlockProperties(
        @DefaultValue("tg:block:") String keyPrefix
) {
}
