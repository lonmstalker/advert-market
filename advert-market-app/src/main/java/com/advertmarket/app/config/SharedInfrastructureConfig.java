package com.advertmarket.app.config;

import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.lock.RedisDistributedLock;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.outbox.OutboxPoller;
import com.advertmarket.shared.outbox.OutboxProperties;
import com.advertmarket.shared.outbox.OutboxPublisher;
import com.advertmarket.shared.outbox.OutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Registers shared-module infrastructure beans that were
 * extracted from the shared kernel (C-7, H-14).
 */
@Configuration
public class SharedInfrastructureConfig {

    @Bean
    public MetricsFacade metricsFacade(MeterRegistry registry) {
        return new MetricsFacade(registry);
    }

    @Bean
    public LocalizationService localizationService(
            MessageSource messageSource) {
        return new LocalizationService(messageSource);
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public DistributedLockPort distributedLock(
            StringRedisTemplate redisTemplate,
            MetricsFacade metrics) {
        return new RedisDistributedLock(redisTemplate, metrics);
    }

    @Bean
    @ConditionalOnBean({OutboxRepository.class,
            OutboxPublisher.class})
    public OutboxPoller outboxPoller(
            OutboxRepository repository,
            OutboxPublisher publisher,
            OutboxProperties properties,
            MetricsFacade metrics) {
        return new OutboxPoller(
                repository, publisher, properties, metrics);
    }
}