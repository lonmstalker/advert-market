package com.advertmarket.shared.deploy;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Canary deployment configuration properties. */
@ConfigurationProperties(prefix = "app.canary")
@PropertyGroupDoc(
        displayName = "Canary Deployment",
        description = "Feature-flag canary routing configuration",
        category = "Deploy"
)
public record CanaryProperties(
        @PropertyDoc(
                description = "Static bearer token for canary admin API",
                required = Requirement.REQUIRED,
                sensitive = true
        )
        String adminToken
) {
}