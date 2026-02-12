package com.advertmarket.app.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * CORS configuration for Telegram Mini App.
 *
 * @param allowedOrigins allowed origins for CORS requests
 */
@ConfigurationProperties(prefix = "app.cors")
@Validated
@PropertyGroupDoc(
        displayName = "CORS",
        description = "CORS configuration for Telegram Mini App",
        category = "Security"
)
public record CorsProperties(
        @PropertyDoc(
                description = "Allowed origins for CORS requests",
                required = Requirement.REQUIRED
        )
        @NotEmpty List<String> allowedOrigins
) {
}
