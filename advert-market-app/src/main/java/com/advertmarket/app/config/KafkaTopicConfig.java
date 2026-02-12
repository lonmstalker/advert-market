package com.advertmarket.app.config;

import com.advertmarket.shared.event.TopicNames;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka topic auto-creation configuration.
 */
@Configuration
public class KafkaTopicConfig {

    private static final int DEFAULT_PARTITIONS = 3;
    private static final int FINANCIAL_PARTITIONS = 6;
    private static final short REPLICATION = 1;

    /** Deal state change events topic. */
    @Bean
    public NewTopic dealStateChangedTopic() {
        return new NewTopic(TopicNames.DEAL_STATE_CHANGED,
                DEFAULT_PARTITIONS, REPLICATION);
    }

    /** Deal deadlines topic. */
    @Bean
    public NewTopic dealDeadlinesTopic() {
        return new NewTopic(TopicNames.DEAL_DEADLINES,
                DEFAULT_PARTITIONS, REPLICATION);
    }

    /** Financial commands topic. */
    @Bean
    public NewTopic financialCommandsTopic() {
        return new NewTopic(TopicNames.FINANCIAL_COMMANDS,
                FINANCIAL_PARTITIONS, REPLICATION);
    }

    /** Financial events topic. */
    @Bean
    public NewTopic financialEventsTopic() {
        return new NewTopic(TopicNames.FINANCIAL_EVENTS,
                FINANCIAL_PARTITIONS, REPLICATION);
    }

    /** Delivery commands topic. */
    @Bean
    public NewTopic deliveryCommandsTopic() {
        return new NewTopic(TopicNames.DELIVERY_COMMANDS,
                DEFAULT_PARTITIONS, REPLICATION);
    }

    /** Delivery events topic. */
    @Bean
    public NewTopic deliveryEventsTopic() {
        return new NewTopic(TopicNames.DELIVERY_EVENTS,
                DEFAULT_PARTITIONS, REPLICATION);
    }

    /** Communication notifications topic. */
    @Bean
    public NewTopic communicationNotificationsTopic() {
        return new NewTopic(
                TopicNames.COMMUNICATION_NOTIFICATIONS,
                DEFAULT_PARTITIONS, REPLICATION);
    }

    /** Financial reconciliation topic. */
    @Bean
    public NewTopic financialReconciliationTopic() {
        return new NewTopic(
                TopicNames.FINANCIAL_RECONCILIATION,
                1, REPLICATION);
    }
}
