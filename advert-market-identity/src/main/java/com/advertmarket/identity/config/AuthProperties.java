package com.advertmarket.identity.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Authentication configuration properties.
 *
 * @param jwt                      JWT token settings
 * @param antiReplayWindowSeconds  maximum age of initData auth_date in seconds
 */
@ConfigurationProperties(prefix = "app.auth")
@PropertyGroupDoc(
        displayName = "Authentication",
        description = "JWT and Telegram initData validation settings",
        category = "Security"
)
@Validated
public record AuthProperties(
        @PropertyDoc(
                description = "JWT token configuration",
                required = Requirement.REQUIRED
        )
        @Valid Jwt jwt,

        @PropertyDoc(
                description = "Maximum age of Telegram initData auth_date"
                        + " in seconds for anti-replay protection",
                required = Requirement.REQUIRED
        )
        @Positive int antiReplayWindowSeconds
) {

    /**
     * JWT settings.
     *
     * @param secret     signing secret (minimum 32 bytes for HS256)
     * @param expiration token lifetime in seconds
     */
    public record Jwt(
            @PropertyDoc(
                    description = "HS256 signing secret"
                            + " (minimum 32 bytes)",
                    required = Requirement.REQUIRED,
                    sensitive = true
            )
            @NotBlank @Size(min = 32) String secret,

            @PropertyDoc(
                    description = "Token lifetime in seconds",
                    required = Requirement.REQUIRED
            )
            @Positive long expiration
    ) {
    }
}
