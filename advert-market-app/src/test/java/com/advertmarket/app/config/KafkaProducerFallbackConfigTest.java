package com.advertmarket.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

@DisplayName("KafkaProducerFallbackConfig")
class KafkaProducerFallbackConfigTest {

    private final KafkaProducerFallbackConfig config =
            new KafkaProducerFallbackConfig();

    @Test
    @DisplayName("Creates producer factory with bootstrap and string serializers")
    void producerFactory() {
        var producerFactory = config.producerFactory(
                new KafkaClientProperties("kafka:9092"));
        assertThat(producerFactory)
                .isInstanceOf(DefaultKafkaProducerFactory.class);

        @SuppressWarnings("unchecked")
        var defaultFactory =
                (DefaultKafkaProducerFactory<String, String>)
                        producerFactory;

        var props = defaultFactory.getConfigurationProperties();
        assertThat(props).containsEntry(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "kafka:9092");
        assertThat(props).containsEntry(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        assertThat(props).containsEntry(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
    }

    @Test
    @DisplayName("Creates kafka template from producer factory")
    void kafkaTemplate() {
        var producerFactory = config.producerFactory(
                new KafkaClientProperties("localhost:9092"));
        var kafkaTemplate = config.kafkaTemplate(producerFactory);
        assertThat(kafkaTemplate).isNotNull();
    }
}
