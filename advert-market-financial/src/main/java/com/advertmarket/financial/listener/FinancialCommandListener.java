package com.advertmarket.financial.listener;

import com.advertmarket.financial.api.event.ExecutePayoutCommand;
import com.advertmarket.financial.api.event.ExecuteRefundCommand;
import com.advertmarket.financial.api.event.WatchDepositCommand;
import com.advertmarket.financial.api.port.PayoutExecutorPort;
import com.advertmarket.financial.api.port.RefundExecutorPort;
import com.advertmarket.financial.ton.service.DepositWatcher;
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
 * Kafka listener for financial commands (payout, refund).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialCommandListener {

    private final EventEnvelopeDeserializer deserializer;
    private final PayoutExecutorPort payoutExecutor;
    private final RefundExecutorPort refundExecutor;
    private final DepositWatcher depositWatcher;
    private final MetricsFacade metrics;

    /** Consumes financial commands from Kafka. */
    @SuppressWarnings("fenum")
    @KafkaListener(
            topics = TopicNames.FINANCIAL_COMMANDS,
            groupId = ConsumerGroups.FINANCIAL_COMMAND_HANDLER,
            containerFactory = "financialKafkaListenerContainerFactory")
    public void onMessage(
            ConsumerRecord<String, String> record,
            Acknowledgment ack) {
        var envelope = deserializer.deserialize(record.value());
        log.info("Financial command received: type={}, eventId={}, dealId={}",
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
            case EventTypes.WATCH_DEPOSIT ->
                    depositWatcher.watchDeposit(
                            (EventEnvelope<WatchDepositCommand>) envelope);
            case EventTypes.EXECUTE_PAYOUT ->
                    payoutExecutor.executePayout(
                            (EventEnvelope<ExecutePayoutCommand>) envelope);
            case EventTypes.EXECUTE_REFUND ->
                    refundExecutor.executeRefund(
                            (EventEnvelope<ExecuteRefundCommand>) envelope);
            default -> log.warn(
                    "Unhandled financial command type: {}",
                    envelope.eventType());
        }
    }
}
