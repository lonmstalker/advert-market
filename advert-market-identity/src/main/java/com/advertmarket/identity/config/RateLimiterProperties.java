package com.advertmarket.identity.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Login rate limiter configuration.
 *
 * @param maxAttempts   maximum login attempts per window
 * @param windowSeconds rate limit window duration in seconds
 */
@ConfigurationProperties(prefix = "app.auth.rate-limiter")
@Validated
@PropertyGroupDoc(
        displayName = "Login Rate Limiter",
        description = "Login rate limiter configuration",
        category = "Security"
)
public record RateLimiterProperties(
        @PropertyDoc(
                description = "Maximum login attempts per window",
                required = Requirement.REQUIRED
        )
        @Positive int maxAttempts,

        @PropertyDoc(
                description = "Rate limit window duration in seconds",
                required = Requirement.REQUIRED
        )
        @Positive int windowSeconds
) {
}
