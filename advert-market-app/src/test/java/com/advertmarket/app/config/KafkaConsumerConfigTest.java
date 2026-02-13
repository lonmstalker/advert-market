package com.advertmarket.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@DisplayName("KafkaConsumerConfig")
class KafkaConsumerConfigTest {

    @Test
    @DisplayName("Reads bootstrap servers from KafkaClientProperties")
    void consumerFactoryUsesKafkaProperties() {
        var config = new KafkaConsumerConfig(
                new KafkaClientProperties("kafka:9092"));

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
}
