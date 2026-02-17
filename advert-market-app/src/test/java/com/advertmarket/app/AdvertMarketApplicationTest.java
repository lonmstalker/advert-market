package com.advertmarket.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.EnableKafka;

@DisplayName("AdvertMarketApplication")
class AdvertMarketApplicationTest {

    @Test
    @DisplayName("Enables Kafka listeners")
    void enablesKafkaListeners() {
        assertThat(AdvertMarketApplication.class
                .isAnnotationPresent(EnableKafka.class))
                .isTrue();
    }
}
