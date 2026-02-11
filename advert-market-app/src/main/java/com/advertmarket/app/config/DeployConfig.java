package com.advertmarket.app.config;

import com.advertmarket.shared.deploy.CanaryProperties;
import com.advertmarket.shared.deploy.DeployProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CanaryProperties.class, DeployProperties.class})
public class DeployConfig {
}