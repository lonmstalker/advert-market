package com.advertmarket.deal.listener;

import com.advertmarket.deal.api.event.DealStateChangedEvent;
import com.advertmarket.deal.service.DealWorkflowEngine;
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
 * Kafka listener for deal state changes, dispatching post-transition workflow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DealStateChangedListener {

    private final EventEnvelopeDeserializer deserializer;
    private final DealWorkflowEngine workflowEngine;
    private final MetricsFacade metrics;

    @SuppressWarnings("fenum")
    @KafkaListener(
            topics = TopicNames.DEAL_STATE_CHANGED,
            groupId = ConsumerGroups.DEAL_WORKFLOW_ENGINE,
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(
            ConsumerRecord<String, String> record,
            Acknowledgment ack) {
        var envelope = deserializer.deserialize(record.value());
        metrics.incrementCounter(
                MetricNames.WORKER_EVENT_RECEIVED,
                "type",
                envelope.eventType());

        if (!EventTypes.DEAL_STATE_CHANGED.equals(envelope.eventType())) {
            log.warn("Unexpected event type on deal.state-changed topic: {}",
                    envelope.eventType());
            ack.acknowledge();
            return;
        }

        @SuppressWarnings("unchecked")
        var typed = (EventEnvelope<DealStateChangedEvent>) envelope;
        try {
            workflowEngine.handle(typed);
        } catch (RuntimeException ex) {
            log.error(
                    "Failed to handle deal.state-changed event: eventId={}, dealId={}, toStatus={}",
                    typed.eventId(),
                    typed.dealId(),
                    typed.payload().toStatus(),
                    ex);
            throw ex;
        }
        ack.acknowledge();
    }
}
