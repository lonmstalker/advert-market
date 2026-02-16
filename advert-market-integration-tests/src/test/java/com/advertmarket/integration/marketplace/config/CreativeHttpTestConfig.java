package com.advertmarket.integration.marketplace.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Spring test configuration for creative HTTP integration tests.
 */
@Configuration
@EnableAutoConfiguration
@Import(MarketplaceTestConfig.class)
@ComponentScan(basePackages = "com.advertmarket.marketplace.creative")
public class CreativeHttpTestConfig {
}
