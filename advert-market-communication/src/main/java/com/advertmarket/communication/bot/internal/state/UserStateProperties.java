package com.advertmarket.communication.bot.internal.state;

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
@Validated
public record UserStateProperties(
        @DefaultValue("1h") Duration defaultTtl,
        @DefaultValue("tg:state:") String keyPrefix
) {
}
