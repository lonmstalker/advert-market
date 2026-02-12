package com.advertmarket.communication.channel.internal;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Telegram channel cache.
 *
 * @param chatInfoTtl TTL for cached chat info entries
 * @param adminsTtl   TTL for cached administrator list entries
 * @param keyPrefix   Redis key prefix for channel cache
 */
@ConfigurationProperties(prefix = "app.telegram.channel.cache")
@PropertyGroupDoc(
        displayName = "Channel Cache",
        description = "Redis cache for Telegram channel API responses",
        category = "Telegram"
)
@Validated
public record ChannelCacheProperties(
        @PropertyDoc(
                description = "TTL for cached chat info entries",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("5m") Duration chatInfoTtl,

        @PropertyDoc(
                description = "TTL for cached administrator list entries",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("15m") Duration adminsTtl,

        @PropertyDoc(
                description = "Redis key prefix for channel cache",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("tg:chan:cache:") String keyPrefix
) {
}
