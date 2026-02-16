package com.advertmarket.app.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.advertmarket.financial.api.event.DepositConfirmedEvent;
import com.advertmarket.financial.api.event.DepositFailedEvent;
import com.advertmarket.financial.api.event.DepositFailureReason;
import com.advertmarket.financial.api.event.PayoutCompletedEvent;
import com.advertmarket.financial.api.event.RefundCompletedEvent;
import com.advertmarket.financial.api.port.FinancialEventPort;
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

@DisplayName("FinancialEventListener")
@ExtendWith(MockitoExtension.class)
class FinancialEventListenerTest {

    @Mock
    private EventEnvelopeDeserializer deserializer;
    @Mock
    private FinancialEventPort financialEventPort;
    @Mock
    private MetricsFacade metrics;
    @Mock
    private Acknowledgment ack;
    @InjectMocks
    private FinancialEventListener listener;

    private static final DealId DEAL_ID = DealId.of(UUID.randomUUID());

    @Test
    @DisplayName("Dispatches DEPOSIT_CONFIRMED to port")
    void dispatchesDepositConfirmed() {
        var payload = new DepositConfirmedEvent(
                "tx1", 1_000_000_000L, 1_000_000_000L,
                3, "from", "deposit");
        var envelope = EventEnvelope.create(
                EventTypes.DEPOSIT_CONFIRMED, DEAL_ID, payload);
        var record = new ConsumerRecord<>("topic", 0, 0L,
                "key", "json");
        doReturn(envelope).when(deserializer).deserialize("json");

        listener.onMessage(record, ack);

        verify(financialEventPort).onDepositConfirmed(any());
        verify(ack).acknowledge();
        verify(metrics).incrementCounter(
                eq(MetricNames.WORKER_EVENT_RECEIVED),
                eq("type"), eq(EventTypes.DEPOSIT_CONFIRMED));
    }

    @Test
    @DisplayName("Dispatches DEPOSIT_FAILED to port")
    void dispatchesDepositFailed() {
        var payload = new DepositFailedEvent(
                DepositFailureReason.TIMEOUT, 1_000_000_000L, 0L);
        var envelope = EventEnvelope.create(
                EventTypes.DEPOSIT_FAILED, DEAL_ID, payload);
        var record = new ConsumerRecord<>("topic", 0, 0L,
                "key", "json");
        doReturn(envelope).when(deserializer).deserialize("json");

        listener.onMessage(record, ack);

        verify(financialEventPort).onDepositFailed(any());
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("Dispatches PAYOUT_COMPLETED to port")
    void dispatchesPayoutCompleted() {
        var payload = new PayoutCompletedEvent(
                "tx2", 900_000_000L, 50_000_000L, "toAddr", 5);
        var envelope = EventEnvelope.create(
                EventTypes.PAYOUT_COMPLETED, DEAL_ID, payload);
        var record = new ConsumerRecord<>("topic", 0, 0L,
                "key", "json");
        doReturn(envelope).when(deserializer).deserialize("json");

        listener.onMessage(record, ack);

        verify(financialEventPort).onPayoutCompleted(any());
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("Dispatches REFUND_COMPLETED to port")
    void dispatchesRefundCompleted() {
        var payload = new RefundCompletedEvent(
                "tx3", 500_000_000L, "refundAddr", 3);
        var envelope = EventEnvelope.create(
                EventTypes.REFUND_COMPLETED, DEAL_ID, payload);
        var record = new ConsumerRecord<>("topic", 0, 0L,
                "key", "json");
        doReturn(envelope).when(deserializer).deserialize("json");

        listener.onMessage(record, ack);

        verify(financialEventPort).onRefundCompleted(any());
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("Unknown type does not invoke port")
    void unknownType_doesNotInvokePort() {
        var payload = new DepositConfirmedEvent(
                "tx1", 1_000_000_000L, 1_000_000_000L,
                3, "from", "deposit");
        var envelope = new EventEnvelope<>(
                UUID.randomUUID(), "UNKNOWN_TYPE", DEAL_ID,
                Instant.now(), 1,
                UUID.randomUUID(), payload);
        var record = new ConsumerRecord<>("topic", 0, 0L,
                "key", "json");
        doReturn(envelope).when(deserializer).deserialize("json");

        listener.onMessage(record, ack);

        verifyNoInteractions(financialEventPort);
        verify(ack).acknowledge();
    }
}
