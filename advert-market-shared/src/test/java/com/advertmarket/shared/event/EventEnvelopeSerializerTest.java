package com.advertmarket.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.json.JsonFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EventEnvelopeSerializer")
class EventEnvelopeSerializerTest {

    private record TestEvent(String data) implements DomainEvent {
    }

    private EventEnvelopeSerializer serializer;

    @BeforeEach
    void setUp() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        serializer = new EventEnvelopeSerializer(
                new JsonFacade(mapper));
    }

    @Test
    @DisplayName("Serializes envelope to valid JSON bytes")
    void serialize_producesValidJson() {
        var envelope = EventEnvelope.create(
                "TEST", null, new TestEvent("hello"));

        byte[] bytes = serializer.serialize(envelope);
        var json = new String(bytes, StandardCharsets.UTF_8);

        assertThat(json).contains("\"eventType\":\"TEST\"");
        assertThat(json).contains("\"data\":\"hello\"");
        assertThat(json).contains("\"eventId\"");
        assertThat(json).contains("\"correlationId\"");
    }

    @Test
    @DisplayName("Serializes envelope to string")
    void serializeToString_producesValidJson() {
        var envelope = EventEnvelope.create(
                "TEST", null, new TestEvent("world"));

        String json = serializer.serializeToString(envelope);

        assertThat(json).contains("\"eventType\":\"TEST\"");
        assertThat(json).contains("\"data\":\"world\"");
    }
}
