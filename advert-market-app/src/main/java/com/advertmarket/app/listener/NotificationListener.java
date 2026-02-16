package com.advertmarket.app.listener;

import com.advertmarket.communication.api.event.NotificationEvent;
import com.advertmarket.communication.api.notification.NotificationPort;
import com.advertmarket.communication.api.notification.NotificationRequest;
import com.advertmarket.communication.api.notification.NotificationType;
import com.advertmarket.shared.event.ConsumerGroups;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventEnvelopeDeserializer;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka listener that delivers notifications to Telegram.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final EventEnvelopeDeserializer deserializer;
    private final NotificationPort notificationPort;
    private final MetricsFacade metrics;

    /** Consumes notification events and sends them via Telegram. */
    @SuppressWarnings("fenum")
    @KafkaListener(
            topics = TopicNames.COMMUNICATION_NOTIFICATIONS,
            groupId = ConsumerGroups.NOTIFICATION_SENDER,
            containerFactory = "notificationKafkaListenerContainerFactory")
    public void onMessage(
            ConsumerRecord<String, String> record,
            Acknowledgment ack) {
        var envelope = deserializer.deserialize(record.value());

        if (!EventTypes.NOTIFICATION.equals(envelope.eventType())) {
            log.warn("Unexpected event type on notifications topic: {}",
                    envelope.eventType());
            ack.acknowledge();
            return;
        }

        @SuppressWarnings("unchecked")
        var event = ((EventEnvelope<NotificationEvent>) envelope)
                .payload();

        deliver(event);
        ack.acknowledge();
    }

    private void deliver(NotificationEvent event) {
        NotificationType type;
        try {
            type = NotificationType.valueOf(event.template());
        } catch (IllegalArgumentException e) {
            log.error("Unknown notification template: {}",
                    event.template());
            metrics.incrementCounter(MetricNames.NOTIFICATION_FAILED,
                    "type", event.template());
            return;
        }

        var request = new NotificationRequest(
                event.recipientId(), type, event.vars());

        boolean sent = notificationPort.send(request);
        if (sent) {
            metrics.incrementCounter(MetricNames.NOTIFICATION_DELIVERED,
                    "type", event.template());
        } else {
            metrics.incrementCounter(MetricNames.NOTIFICATION_FAILED,
                    "type", event.template());
            log.warn("Failed to deliver notification type={} to user={}",
                    event.template(), event.recipientId());
        }
    }
}
