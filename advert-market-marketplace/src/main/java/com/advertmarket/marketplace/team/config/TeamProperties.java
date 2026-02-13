package com.advertmarket.marketplace.team.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for channel team management limits.
 *
 * @param maxManagers maximum number of managers per channel
 */
@ConfigurationProperties(prefix = "app.marketplace.team")
@PropertyGroupDoc(
        displayName = "Team Management",
        description = "Limits for channel team management",
        category = "Marketplace"
)
@Validated
public record TeamProperties(
        @PropertyDoc(
                description = "Maximum number of managers per channel",
                required = Requirement.REQUIRED
        )
        @Positive int maxManagers
) {
}
