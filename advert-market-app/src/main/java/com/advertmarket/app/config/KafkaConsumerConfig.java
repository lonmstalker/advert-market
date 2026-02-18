package com.advertmarket.app.config;

import com.advertmarket.app.listener.KafkaMdcInterceptor;
import com.advertmarket.shared.event.EventDeserializationException;
import com.advertmarket.shared.json.JsonException;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

/**
 * Kafka consumer infrastructure with three retry profiles.
 */
@Configuration
@EnableConfigurationProperties(KafkaClientProperties.class)
public class KafkaConsumerConfig {

    private static final long DEFAULT_INITIAL_MS = 1_000L;
    private static final long DEFAULT_MAX_MS = 30_000L;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final int DEFAULT_RETRIES = 3;
    private static final long FINANCIAL_INITIAL_MS = 5_000L;
    private static final long FINANCIAL_MAX_MS = 60_000L;
    private static final int FINANCIAL_RETRIES = 5;
    private static final long NOTIFICATION_MAX_MS = 10_000L;
    private static final int NOTIFICATION_RETRIES = 3;

    // CHECKSTYLE.SUPPRESS: AbbreviationAsWordInName for +1 lines
    private final String bootstrapServers;
    private final MetricsFacade metrics;

    KafkaConsumerConfig(
            KafkaClientProperties kafkaProperties,
            MetricsFacade metrics) {
        this.bootstrapServers = kafkaProperties.bootstrapServers();
        this.metrics = metrics;
    }

    /** Consumer factory with error-handling deserializer. */
    @Bean
    public ConsumerFactory<@NonNull String, @NonNull String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);
        props.put(
                ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,
                StringDeserializer.class);
        props.put(
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /** Default listener container factory (3 retries, 1-30s). */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        return buildFactory(consumerFactory,
                defaultErrorHandler(kafkaTemplate));
    }

    /** Financial listener factory (5 retries, 5-60s). */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            financialKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        return buildFactory(consumerFactory,
                financialErrorHandler(kafkaTemplate));
    }

    /** Notification listener factory (3 retries, 1-10s). */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            notificationKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        return buildFactory(consumerFactory,
                notificationErrorHandler(kafkaTemplate));
    }

    private ConcurrentKafkaListenerContainerFactory<String, String>
            buildFactory(
            ConsumerFactory<String, String> consumerFactory,
            CommonErrorHandler errorHandler) {
        var factory =
                new ConcurrentKafkaListenerContainerFactory<
                        String, String>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler);
        factory.setRecordInterceptor(new KafkaMdcInterceptor());
        return factory;
    }

    private DefaultErrorHandler defaultErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate) {
        return createHandler(kafkaTemplate,
                DEFAULT_RETRIES,
                DEFAULT_INITIAL_MS, BACKOFF_MULTIPLIER,
                DEFAULT_MAX_MS);
    }

    private DefaultErrorHandler financialErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate) {
        return createHandler(kafkaTemplate,
                FINANCIAL_RETRIES,
                FINANCIAL_INITIAL_MS, BACKOFF_MULTIPLIER,
                FINANCIAL_MAX_MS);
    }

    private DefaultErrorHandler notificationErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate) {
        return createHandler(kafkaTemplate,
                NOTIFICATION_RETRIES,
                DEFAULT_INITIAL_MS, BACKOFF_MULTIPLIER,
                NOTIFICATION_MAX_MS);
    }

    @SuppressWarnings("fenum:argument")
    private DefaultErrorHandler createHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            int maxRetries,
            long initialInterval,
            double multiplier,
            long maxInterval) {
        var dlqRecoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate);
        var backOff = new ExponentialBackOffWithMaxRetries(
                maxRetries);
        backOff.setInitialInterval(initialInterval);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxInterval);
        var handler = new DefaultErrorHandler(
                (record, ex) -> {
                    metrics.incrementCounter(MetricNames.DLQ_EVENT_SENT);
                    dlqRecoverer.accept(record, ex);
                }, backOff);
        handler.addNotRetryableExceptions(
                EventDeserializationException.class,
                JsonException.class);
        return handler;
    }
}
