package com.advertmarket.shared.deploy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.deploy")
public record DeployProperties(
        String instanceId,
        String color
) {
}