package com.advertmarket.app.listener;

import com.advertmarket.delivery.api.event.DeliveryFailedEvent;
import com.advertmarket.delivery.api.event.DeliveryVerifiedEvent;
import com.advertmarket.delivery.api.event.PublicationResultEvent;
import com.advertmarket.delivery.api.port.DeliveryEventPort;
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
 * Kafka listener for delivery result events from workers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventListener {

    private final EventEnvelopeDeserializer deserializer;
    private final DeliveryEventPort deliveryEventPort;
    private final MetricsFacade metrics;

    /** Consumes delivery events from Kafka. */
    @SuppressWarnings("fenum")
    @KafkaListener(
            topics = TopicNames.DELIVERY_EVENTS,
            groupId = ConsumerGroups.DELIVERY_EVENT_HANDLER,
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(
            ConsumerRecord<String, String> record,
            Acknowledgment ack) {
        var envelope = deserializer.deserialize(record.value());
        log.info("Delivery event received: type={}, eventId={}, dealId={}",
                envelope.eventType(), envelope.eventId(),
                envelope.dealId());

        metrics.incrementCounter(MetricNames.WORKER_EVENT_RECEIVED,
                "type", envelope.eventType());

        dispatch(envelope);
        ack.acknowledge();
    }

    @SuppressWarnings({"unchecked", "fenum"})
    private void dispatch(EventEnvelope<?> envelope) {
        switch (envelope.eventType()) {
            case EventTypes.PUBLICATION_RESULT ->
                    deliveryEventPort.onPublicationResult(
                            (EventEnvelope<PublicationResultEvent>) envelope);
            case EventTypes.DELIVERY_VERIFIED ->
                    deliveryEventPort.onDeliveryVerified(
                            (EventEnvelope<DeliveryVerifiedEvent>) envelope);
            case EventTypes.DELIVERY_FAILED ->
                    deliveryEventPort.onDeliveryFailed(
                            (EventEnvelope<DeliveryFailedEvent>) envelope);
            default -> log.warn(
                    "Unknown delivery event type: {}",
                    envelope.eventType());
        }
    }
}
