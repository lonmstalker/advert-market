package com.advertmarket.shared.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.DealId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EventEnvelopeDeserializer")
class EventEnvelopeDeserializerTest {

    private record TestEvent(String data) implements DomainEvent {
    }

    private EventEnvelopeSerializer serializer;
    private EventEnvelopeDeserializer deserializer;

    @BeforeEach
    void setUp() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        var json = new JsonFacade(mapper);

        var registry = new EventTypeRegistry();
        registry.register("TEST", TestEvent.class);

        serializer = new EventEnvelopeSerializer(json);
        deserializer = new EventEnvelopeDeserializer(
                json, registry);
    }

    @Test
    @DisplayName("Roundtrip serialize/deserialize preserves data")
    void roundtrip_preservesData() {
        var original = EventEnvelope.create(
                "TEST", null, new TestEvent("hello"));

        byte[] bytes = serializer.serialize(original);
        var restored = deserializer.deserialize(bytes);

        assertThat(restored.eventId())
                .isEqualTo(original.eventId());
        assertThat(restored.eventType()).isEqualTo("TEST");
        assertThat(restored.version())
                .isEqualTo(original.version());
        assertThat(restored.payload())
                .isEqualTo(new TestEvent("hello"));
    }

    @Test
    @DisplayName("Roundtrip with string")
    void roundtripString_preservesData() {
        var original = EventEnvelope.create(
                "TEST", null, new TestEvent("world"));

        String json = serializer.serializeToString(original);
        var restored = deserializer.deserialize(json);

        assertThat(restored.eventType()).isEqualTo("TEST");
        assertThat(restored.payload())
                .isEqualTo(new TestEvent("world"));
    }

    @Test
    @DisplayName("Unknown event type throws exception")
    void unknownEventType_throws() {
        String json = """
                {"eventId":"00000000-0000-0000-0000-000000000001",\
                "eventType":"UNKNOWN",\
                "timestamp":"2025-01-01T00:00:00Z",\
                "version":1,\
                "correlationId":"00000000-0000-0000-0000-000000000002",\
                "payload":{"data":"x"}}""";

        assertThatThrownBy(() -> deserializer.deserialize(json))
                .isInstanceOf(EventDeserializationException.class)
                .hasMessageContaining("Unknown event type");
    }

    @Test
    @DisplayName("Malformed JSON throws exception")
    void malformedJson_throws() {
        assertThatThrownBy(
                () -> deserializer.deserialize("{invalid"))
                .isInstanceOf(
                        EventDeserializationException.class)
                .hasMessageContaining("Malformed JSON");
    }

    @Test
    @DisplayName("Roundtrip with non-null dealId preserves it")
    void roundtripWithDealId_preservesDealId() {
        var dealId = DealId.generate();
        var original = EventEnvelope.create(
                "TEST", dealId, new TestEvent("deal"));

        String json = serializer.serializeToString(original);
        var restored = deserializer.deserialize(json);

        assertThat(restored.dealId()).isEqualTo(dealId);
    }

    @Test
    @DisplayName("Exception carries EVENT_DESERIALIZATION_ERROR code")
    void exceptionCarriesErrorCode() {
        assertThatThrownBy(
                () -> deserializer.deserialize("{invalid"))
                .isInstanceOf(
                        EventDeserializationException.class)
                .extracting("errorCode")
                .isEqualTo("EVENT_DESERIALIZATION_ERROR");
    }
}
