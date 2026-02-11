package com.advertmarket.shared.deploy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.canary")
public record CanaryProperties(
        String adminToken
) {
}