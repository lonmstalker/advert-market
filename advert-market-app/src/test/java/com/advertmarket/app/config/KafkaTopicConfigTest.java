package com.advertmarket.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.event.TopicNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KafkaTopicConfig")
class KafkaTopicConfigTest {

    private final KafkaTopicConfig config = new KafkaTopicConfig();

    @Test
    @DisplayName("Creates deal state changed topic")
    void dealStateChangedTopic() {
        var topic = config.dealStateChangedTopic();
        assertThat(topic.name())
                .isEqualTo(TopicNames.DEAL_STATE_CHANGED);
        assertThat(topic.numPartitions()).isEqualTo(3);
    }

    @Test
    @DisplayName("Creates deal deadlines topic")
    void dealDeadlinesTopic() {
        var topic = config.dealDeadlinesTopic();
        assertThat(topic.name())
                .isEqualTo(TopicNames.DEAL_DEADLINES);
        assertThat(topic.numPartitions()).isEqualTo(3);
    }

    @Test
    @DisplayName("Financial commands has 6 partitions")
    void financialCommandsTopic() {
        var topic = config.financialCommandsTopic();
        assertThat(topic.name())
                .isEqualTo(TopicNames.FINANCIAL_COMMANDS);
        assertThat(topic.numPartitions()).isEqualTo(6);
    }

    @Test
    @DisplayName("Financial events has 6 partitions")
    void financialEventsTopic() {
        var topic = config.financialEventsTopic();
        assertThat(topic.name())
                .isEqualTo(TopicNames.FINANCIAL_EVENTS);
        assertThat(topic.numPartitions()).isEqualTo(6);
    }

    @Test
    @DisplayName("Creates delivery commands topic")
    void deliveryCommandsTopic() {
        var topic = config.deliveryCommandsTopic();
        assertThat(topic.name())
                .isEqualTo(TopicNames.DELIVERY_COMMANDS);
    }

    @Test
    @DisplayName("Creates delivery events topic")
    void deliveryEventsTopic() {
        var topic = config.deliveryEventsTopic();
        assertThat(topic.name())
                .isEqualTo(TopicNames.DELIVERY_EVENTS);
    }

    @Test
    @DisplayName("Creates notifications topic")
    void communicationNotificationsTopic() {
        var topic = config.communicationNotificationsTopic();
        assertThat(topic.name())
                .isEqualTo(TopicNames.COMMUNICATION_NOTIFICATIONS);
    }

    @Test
    @DisplayName("Reconciliation topic has 1 partition")
    void financialReconciliationTopic() {
        var topic = config.financialReconciliationTopic();
        assertThat(topic.name())
                .isEqualTo(TopicNames.FINANCIAL_RECONCILIATION);
        assertThat(topic.numPartitions()).isEqualTo(1);
    }
}
