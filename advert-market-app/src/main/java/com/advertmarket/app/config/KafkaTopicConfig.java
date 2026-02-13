package com.advertmarket.app.config;

import com.advertmarket.shared.FenumGroup;
import com.advertmarket.shared.event.TopicNames;
import org.apache.kafka.clients.admin.NewTopic;
import org.checkerframework.checker.fenum.qual.Fenum;
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
        return topic(TopicNames.DEAL_STATE_CHANGED,
                DEFAULT_PARTITIONS);
    }

    /** Deal deadlines topic. */
    @Bean
    public NewTopic dealDeadlinesTopic() {
        return topic(TopicNames.DEAL_DEADLINES,
                DEFAULT_PARTITIONS);
    }

    /** Financial commands topic. */
    @Bean
    public NewTopic financialCommandsTopic() {
        return topic(TopicNames.FINANCIAL_COMMANDS,
                FINANCIAL_PARTITIONS);
    }

    /** Financial events topic. */
    @Bean
    public NewTopic financialEventsTopic() {
        return topic(TopicNames.FINANCIAL_EVENTS,
                FINANCIAL_PARTITIONS);
    }

    /** Delivery commands topic. */
    @Bean
    public NewTopic deliveryCommandsTopic() {
        return topic(TopicNames.DELIVERY_COMMANDS,
                DEFAULT_PARTITIONS);
    }

    /** Delivery events topic. */
    @Bean
    public NewTopic deliveryEventsTopic() {
        return topic(TopicNames.DELIVERY_EVENTS,
                DEFAULT_PARTITIONS);
    }

    /** Communication notifications topic. */
    @Bean
    public NewTopic communicationNotificationsTopic() {
        return topic(TopicNames.COMMUNICATION_NOTIFICATIONS,
                DEFAULT_PARTITIONS);
    }

    /** Financial reconciliation topic. */
    @Bean
    public NewTopic financialReconciliationTopic() {
        return topic(TopicNames.FINANCIAL_RECONCILIATION, 1);
    }

    @SuppressWarnings("fenum:argument")
    private static NewTopic topic(
            @Fenum(FenumGroup.TOPIC_NAME) String name,
            int partitions) {
        return new NewTopic(name, partitions, REPLICATION);
    }
}
