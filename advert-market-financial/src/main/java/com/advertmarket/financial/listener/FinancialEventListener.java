package com.advertmarket.financial.listener;

import com.advertmarket.financial.api.event.DepositConfirmedEvent;
import com.advertmarket.financial.api.event.DepositFailedEvent;
import com.advertmarket.financial.api.event.PayoutCompletedEvent;
import com.advertmarket.financial.api.event.RefundCompletedEvent;
import com.advertmarket.financial.api.port.FinancialEventPort;
import com.advertmarket.shared.event.ConsumerGroups;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventEnvelopeDeserializer;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka listener for financial result events from workers.
 */
@Slf4j
@Component
public class FinancialEventListener {

    private final EventEnvelopeDeserializer deserializer;
    private final FinancialEventPort financialEventPort;
    private final MetricsFacade metrics;

    /** Creates listener with Kafka envelope deserializer and financial port. */
    public FinancialEventListener(
            EventEnvelopeDeserializer deserializer,
            @Qualifier("financialEventAdapter")
            FinancialEventPort financialEventPort,
            MetricsFacade metrics) {
        this.deserializer = deserializer;
        this.financialEventPort = financialEventPort;
        this.metrics = metrics;
    }

    /** Consumes financial events from Kafka. */
    @SuppressWarnings("fenum")
    @KafkaListener(
            topics = TopicNames.FINANCIAL_EVENTS,
            groupId = ConsumerGroups.FINANCIAL_EVENT_HANDLER,
            containerFactory = "financialKafkaListenerContainerFactory")
    public void onMessage(
            ConsumerRecord<String, String> record,
            Acknowledgment ack) {
        var envelope = deserializer.deserialize(record.value());
        log.info("Financial event received: type={}, eventId={}, dealId={}",
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
            case EventTypes.DEPOSIT_CONFIRMED ->
                    financialEventPort.onDepositConfirmed(
                            (EventEnvelope<DepositConfirmedEvent>) envelope);
            case EventTypes.DEPOSIT_FAILED ->
                    financialEventPort.onDepositFailed(
                            (EventEnvelope<DepositFailedEvent>) envelope);
            case EventTypes.PAYOUT_COMPLETED ->
                    financialEventPort.onPayoutCompleted(
                            (EventEnvelope<PayoutCompletedEvent>) envelope);
            case EventTypes.REFUND_COMPLETED ->
                    financialEventPort.onRefundCompleted(
                            (EventEnvelope<RefundCompletedEvent>) envelope);
            default -> log.warn(
                    "Unknown financial event type: {}",
                    envelope.eventType());
        }
    }
}
