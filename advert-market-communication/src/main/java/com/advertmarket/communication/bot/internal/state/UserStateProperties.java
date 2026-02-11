package com.advertmarket.communication.bot.internal.state;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for user state storage.
 *
 * @param defaultTtl default time-to-live for user state
 * @param keyPrefix  Redis key prefix for state entries
 */
@ConfigurationProperties(prefix = "app.telegram.state")
@PropertyGroupDoc(
        displayName = "User State",
        description = "Redis-backed user conversational state storage",
        category = "Telegram"
)
@Validated
public record UserStateProperties(
        @PropertyDoc(
                description = "Default TTL for user state entries",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("1h") Duration defaultTtl,

        @PropertyDoc(
                description = "Redis key prefix for state entries",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("tg:state:") String keyPrefix
) {
}
