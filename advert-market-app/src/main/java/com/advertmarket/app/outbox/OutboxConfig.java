package com.advertmarket.app.outbox;

import com.advertmarket.shared.outbox.OutboxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for the transactional outbox pattern.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfig {
}
