package com.advertmarket.financial.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.advertmarket.financial.api.event.ReconciliationResultEvent;
import com.advertmarket.financial.api.port.ReconciliationResultPort;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventEnvelopeDeserializer;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@DisplayName("ReconciliationResultListener")
@ExtendWith(MockitoExtension.class)
class ReconciliationResultListenerTest {

    @Mock
    private EventEnvelopeDeserializer deserializer;
    @Mock
    private ReconciliationResultPort reconciliationResultPort;
    @Mock
    private MetricsFacade metrics;
    @Mock
    private Acknowledgment ack;
    @InjectMocks
    private ReconciliationResultListener listener;

    @Test
    @DisplayName("Dispatches RECONCILIATION_RESULT to port")
    void dispatchesReconciliationResult() {
        var payload = new ReconciliationResultEvent(
                UUID.randomUUID(), Map.of(), Instant.now());
        var envelope = EventEnvelope.create(
                EventTypes.RECONCILIATION_RESULT, null, payload);
        var record = new ConsumerRecord<>("topic", 0, 0L,
                "key", "json");
        doReturn(envelope).when(deserializer).deserialize("json");

        listener.onMessage(record, ack);

        verify(reconciliationResultPort)
                .onReconciliationResult(any());
        verify(ack).acknowledge();
        verify(metrics).incrementCounter(
                eq(MetricNames.WORKER_EVENT_RECEIVED),
                eq("type"), eq(EventTypes.RECONCILIATION_RESULT));
    }

    @Test
    @DisplayName("Ignores RECONCILIATION_START events")
    void ignoresReconciliationStart() {
        var payload = new ReconciliationResultEvent(
                UUID.randomUUID(), Map.of(), Instant.now());
        var envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EventTypes.RECONCILIATION_START,
                null, Instant.now(), 1,
                UUID.randomUUID(), payload);
        var record = new ConsumerRecord<>("topic", 0, 0L,
                "key", "json");
        doReturn(envelope).when(deserializer).deserialize("json");

        listener.onMessage(record, ack);

        verifyNoInteractions(reconciliationResultPort);
        verify(ack).acknowledge();
    }
}
