package com.advertmarket.financial.listener;

import com.advertmarket.financial.api.event.ReconciliationResultEvent;
import com.advertmarket.financial.api.port.ReconciliationResultPort;
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
 * Kafka listener for reconciliation result events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationResultListener {

    private final EventEnvelopeDeserializer deserializer;
    private final ReconciliationResultPort reconciliationResultPort;
    private final MetricsFacade metrics;

    /** Consumes reconciliation result events from Kafka. */
    @SuppressWarnings({"unchecked", "fenum"})
    @KafkaListener(
            topics = TopicNames.FINANCIAL_RECONCILIATION,
            groupId = ConsumerGroups.RECONCILIATION_RESULT_HANDLER,
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(
            ConsumerRecord<String, String> record,
            Acknowledgment ack) {
        var envelope = deserializer.deserialize(record.value());

        if (!EventTypes.RECONCILIATION_RESULT.equals(
                envelope.eventType())) {
            log.debug("Ignoring non-result event: type={}",
                    envelope.eventType());
            ack.acknowledge();
            return;
        }

        log.info("Reconciliation result received: eventId={}, correlationId={}",
                envelope.eventId(), envelope.correlationId());

        metrics.incrementCounter(MetricNames.WORKER_EVENT_RECEIVED,
                "type", envelope.eventType());

        reconciliationResultPort.onReconciliationResult(
                (EventEnvelope<ReconciliationResultEvent>) envelope);
        ack.acknowledge();
    }
}
