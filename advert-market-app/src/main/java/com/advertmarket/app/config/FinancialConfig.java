package com.advertmarket.app.config;

import com.advertmarket.financial.api.port.BalanceCachePort;
import com.advertmarket.financial.config.LedgerProperties;
import com.advertmarket.financial.ledger.cache.RedisBalanceCache;
import com.advertmarket.shared.metric.MetricsFacade;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Wires financial module beans.
 */
@Configuration
@EnableConfigurationProperties(LedgerProperties.class)
public class FinancialConfig {

    /** Creates Redis-backed balance cache with fail-open semantics. */
    @Bean
    public BalanceCachePort balanceCachePort(
            StringRedisTemplate redisTemplate,
            MetricsFacade metricsFacade,
            LedgerProperties ledgerProperties) {
        return new RedisBalanceCache(
                redisTemplate, metricsFacade,
                ledgerProperties.cacheTtl());
    }
}
