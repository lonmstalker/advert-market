package com.advertmarket.shared.deploy;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyExample;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Deployment instance configuration properties. */
@ConfigurationProperties(prefix = "app.deploy")
@PropertyGroupDoc(
        displayName = "Deployment Instance",
        description = "Blue-green deployment instance identification",
        category = "Deploy"
)
public record DeployProperties(
        @PropertyDoc(
                description = "Unique instance identifier",
                required = Requirement.OPTIONAL
        )
        String instanceId,

        @PropertyDoc(
                description = "Blue-green deployment color",
                required = Requirement.OPTIONAL
        )
        @PropertyExample(value = "blue", description = "Blue instance")
        @PropertyExample(value = "green", description = "Green instance")
        String color
) {
}