package com.advertmarket.app.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.advertmarket.delivery.api.event.DeliveryFailedEvent;
import com.advertmarket.delivery.api.event.DeliveryFailureReason;
import com.advertmarket.delivery.api.event.DeliveryVerifiedEvent;
import com.advertmarket.delivery.api.event.PublicationResultEvent;
import com.advertmarket.delivery.api.port.DeliveryEventPort;
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

@DisplayName("DeliveryEventListener")
@ExtendWith(MockitoExtension.class)
class DeliveryEventListenerTest {

    @Mock
    private EventEnvelopeDeserializer deserializer;
    @Mock
    private DeliveryEventPort deliveryEventPort;
    @Mock
    private MetricsFacade metrics;
    @Mock
    private Acknowledgment ack;
    @InjectMocks
    private DeliveryEventListener listener;

    private static final DealId DEAL_ID = DealId.of(UUID.randomUUID());

    @Test
    @DisplayName("Dispatches PUBLICATION_RESULT to port")
    void dispatchesPublicationResult() {
        var payload = new PublicationResultEvent(
                true, 42L, -1001234567890L,
                "hash", Instant.now(), null, null);
        var envelope = EventEnvelope.create(
                EventTypes.PUBLICATION_RESULT, DEAL_ID, payload);
        var record = new ConsumerRecord<>("topic", 0, 0L,
                "key", "json");
        when(deserializer.deserialize("json")).thenReturn(envelope);

        listener.onMessage(record, ack);

        verify(deliveryEventPort).onPublicationResult(any());
        verify(ack).acknowledge();
        verify(metrics).incrementCounter(
                eq(MetricNames.WORKER_EVENT_RECEIVED),
                eq("type"), eq(EventTypes.PUBLICATION_RESULT));
    }

    @Test
    @DisplayName("Dispatches DELIVERY_VERIFIED to port")
    void dispatchesDeliveryVerified() {
        var payload = new DeliveryVerifiedEvent(
                42L, 3, 0, "hash");
        var envelope = EventEnvelope.create(
                EventTypes.DELIVERY_VERIFIED, DEAL_ID, payload);
        var record = new ConsumerRecord<>("topic", 0, 0L,
                "key", "json");
        when(deserializer.deserialize("json")).thenReturn(envelope);

        listener.onMessage(record, ack);

        verify(deliveryEventPort).onDeliveryVerified(any());
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("Dispatches DELIVERY_FAILED to port")
    void dispatchesDeliveryFailed() {
        var payload = new DeliveryFailedEvent(
                42L, DeliveryFailureReason.POST_DELETED,
                1, Instant.now());
        var envelope = EventEnvelope.create(
                EventTypes.DELIVERY_FAILED, DEAL_ID, payload);
        var record = new ConsumerRecord<>("topic", 0, 0L,
                "key", "json");
        when(deserializer.deserialize("json")).thenReturn(envelope);

        listener.onMessage(record, ack);

        verify(deliveryEventPort).onDeliveryFailed(any());
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("Unknown type does not invoke port")
    void unknownType_doesNotInvokePort() {
        var payload = new DeliveryVerifiedEvent(
                42L, 3, 0, "hash");
        var envelope = new EventEnvelope<>(
                UUID.randomUUID(), "UNKNOWN_TYPE", DEAL_ID,
                Instant.now(), 1, UUID.randomUUID(), payload);
        var record = new ConsumerRecord<>("topic", 0, 0L,
                "key", "json");
        when(deserializer.deserialize("json")).thenReturn(envelope);

        listener.onMessage(record, ack);

        verifyNoInteractions(deliveryEventPort);
        verify(ack).acknowledge();
    }
}
