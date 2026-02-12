package com.advertmarket.shared.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.shared.model.DealId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EventEnvelope domain event wrapper")
class EventEnvelopeTest {

    private record TestEvent(String data)
            implements DomainEvent {
    }

    @Test
    @DisplayName("create() generates IDs and timestamp")
    void create_generatesIdsAndTimestamp() {
        var event = new TestEvent("test");
        var envelope = EventEnvelope.create(
                "TEST_EVENT", null, event);

        assertThat(envelope.eventId()).isNotNull();
        assertThat(envelope.eventType())
                .isEqualTo("TEST_EVENT");
        assertThat(envelope.dealId()).isNull();
        assertThat(envelope.timestamp()).isNotNull();
        assertThat(envelope.version()).isEqualTo(1);
        assertThat(envelope.correlationId()).isNotNull();
        assertThat(envelope.payload()).isEqualTo(event);
    }

    @Test
    @DisplayName("create() with dealId preserves it")
    void create_withDealId_preservesIt() {
        var dealId = DealId.generate();
        var envelope = EventEnvelope.create(
                "DEAL_EVENT", dealId, new TestEvent("data"));

        assertThat(envelope.dealId()).isEqualTo(dealId);
    }

    @Test
    @DisplayName("Version below 1 is rejected")
    void versionBelowOne_isRejected() {
        assertThatThrownBy(() -> new EventEnvelope<>(
                UUID.randomUUID(), "TEST", null,
                Instant.now(), 0, UUID.randomUUID(),
                new TestEvent("x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version must be >= 1");
    }

    @Test
    @DisplayName("Null payload is rejected")
    void nullPayload_isRejected() {
        assertThatThrownBy(() -> EventEnvelope.create(
                "TEST", null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
