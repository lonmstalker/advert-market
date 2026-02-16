package com.advertmarket.financial.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.advertmarket.financial.api.event.ExecutePayoutCommand;
import com.advertmarket.financial.api.event.ExecuteRefundCommand;
import com.advertmarket.financial.api.event.WatchDepositCommand;
import com.advertmarket.financial.api.port.PayoutExecutorPort;
import com.advertmarket.financial.api.port.RefundExecutorPort;
import com.advertmarket.financial.ton.service.DepositWatcher;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventEnvelopeDeserializer;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.DealId;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@DisplayName("FinancialCommandListener")
@ExtendWith(MockitoExtension.class)
class FinancialCommandListenerTest {

    @Mock
    private EventEnvelopeDeserializer deserializer;
    @Mock
    private PayoutExecutorPort payoutExecutor;
    @Mock
    private RefundExecutorPort refundExecutor;
    @Mock
    private DepositWatcher depositWatcher;
    @Mock
    private MetricsFacade metrics;
    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private FinancialCommandListener listener;

    private static final DealId DEAL_ID = DealId.of(UUID.randomUUID());

    @Test
    @DisplayName("Dispatches WATCH_DEPOSIT to DepositWatcher")
    void dispatchesWatchDeposit() {
        var payload = new WatchDepositCommand(
                "UQ_watch",
                1_000_000_000L,
                42L);
        var envelope = EventEnvelope.create(
                EventTypes.WATCH_DEPOSIT,
                DEAL_ID,
                payload);
        var record = new ConsumerRecord<>("topic", 0, 0L, "key", "json");
        doReturn(envelope).when(deserializer).deserialize("json");

        listener.onMessage(record, ack);

        verify(depositWatcher).watchDeposit(any());
        verify(ack).acknowledge();
        verify(metrics).incrementCounter(
                eq(MetricNames.WORKER_EVENT_RECEIVED),
                eq("type"),
                eq(EventTypes.WATCH_DEPOSIT));
    }

    @Test
    @DisplayName("Dispatches EXECUTE_PAYOUT to payout executor")
    void dispatchesExecutePayout() {
        var payload = new ExecutePayoutCommand(10L, 900L, 100L, 7);
        var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_PAYOUT, DEAL_ID, payload);
        var record = new ConsumerRecord<>("topic", 0, 0L, "key", "json");
        doReturn(envelope).when(deserializer).deserialize("json");

        listener.onMessage(record, ack);

        verify(payoutExecutor).executePayout(any());
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("Dispatches EXECUTE_REFUND to refund executor")
    void dispatchesExecuteRefund() {
        var payload = new ExecuteRefundCommand(11L, 500L, "UQ_refund", 7);
        var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, DEAL_ID, payload);
        var record = new ConsumerRecord<>("topic", 0, 0L, "key", "json");
        doReturn(envelope).when(deserializer).deserialize("json");

        listener.onMessage(record, ack);

        verify(refundExecutor).executeRefund(any());
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("Unknown type does not dispatch")
    void unknownType_doesNotDispatch() {
        var payload = new WatchDepositCommand("UQ", 1L, 1L);
        var envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "UNKNOWN_TYPE",
                DEAL_ID,
                Instant.now(),
                1,
                UUID.randomUUID(),
                payload);
        var record = new ConsumerRecord<>("topic", 0, 0L, "key", "json");
        doReturn(envelope).when(deserializer).deserialize("json");

        listener.onMessage(record, ack);

        verifyNoInteractions(payoutExecutor, refundExecutor, depositWatcher);
        verify(ack).acknowledge();
    }
}
