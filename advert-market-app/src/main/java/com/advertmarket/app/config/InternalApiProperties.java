package com.advertmarket.app.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the internal API (worker callbacks).
 *
 * @param apiKey shared secret for authenticating worker requests
 * @param allowedNetworks CIDR networks allowed to access internal endpoints
 */
@ConfigurationProperties(prefix = "app.internal-api")
@Validated
@PropertyGroupDoc(
        displayName = "Internal API",
        description = "Security settings for internal worker callback endpoints",
        category = "Security"
)
public record InternalApiProperties(
        @PropertyDoc(
                description = "Shared secret API key for authenticating worker requests",
                required = Requirement.REQUIRED
        )
        @NotBlank String apiKey,

        @PropertyDoc(
                description = "CIDR networks allowed to access internal endpoints",
                required = Requirement.REQUIRED
        )
        @NotEmpty List<String> allowedNetworks
) {
}
