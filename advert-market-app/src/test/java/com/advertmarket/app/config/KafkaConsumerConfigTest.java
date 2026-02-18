package com.advertmarket.app.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.advertmarket.shared.metric.MetricsFacade;
import java.lang.reflect.Field;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.BackOff;

@DisplayName("KafkaConsumerConfig")
class KafkaConsumerConfigTest {

    @Test
    @DisplayName("Reads bootstrap servers from KafkaClientProperties")
    void consumerFactoryUsesKafkaProperties() {
        var config = new KafkaConsumerConfig(
                new KafkaClientProperties("kafka:9092"),
                mock(MetricsFacade.class));

        var consumerFactory = config.consumerFactory();
        assertThat(consumerFactory)
                .isInstanceOf(DefaultKafkaConsumerFactory.class);

        @SuppressWarnings("unchecked")
        var defaultFactory =
                (DefaultKafkaConsumerFactory<String, String>)
                        consumerFactory;
        var configProps = defaultFactory.getConfigurationProperties();
        assertThat(configProps).containsEntry(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "kafka:9092");
    }

    @Test
    @DisplayName("Configures finite retries for financial consumers")
    void financialFactoryUsesFiniteRetries() throws Exception {
        var config = new KafkaConsumerConfig(
                new KafkaClientProperties("kafka:9092"),
                mock(MetricsFacade.class));
        var kafkaTemplate = mock(KafkaTemplate.class);

        var factory = config.financialKafkaListenerContainerFactory(
                config.consumerFactory(),
                kafkaTemplate);

        var errorHandler = extractDefaultErrorHandler(factory);
        var backOff = extractBackOff(errorHandler);

        assertThat(backOff).isInstanceOf(ExponentialBackOffWithMaxRetries.class);
        var retries = ((ExponentialBackOffWithMaxRetries) backOff)
                .getMaxRetries();
        assertThat(retries).isEqualTo(5);
    }

    private static DefaultErrorHandler extractDefaultErrorHandler(
            Object factory) throws Exception {
        Field commonErrorHandler = findField(
                factory.getClass(), "commonErrorHandler");
        return (DefaultErrorHandler) commonErrorHandler.get(factory);
    }

    private static BackOff extractBackOff(
            DefaultErrorHandler errorHandler) throws Exception {
        Field failureTracker = findField(
                errorHandler.getClass(), "failureTracker");
        Object tracker = failureTracker.get(errorHandler);
        Field backOff = findField(tracker.getClass(), "backOff");
        return (BackOff) backOff.get(tracker);
    }

    private static Field findField(Class<?> type, String name)
            throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
