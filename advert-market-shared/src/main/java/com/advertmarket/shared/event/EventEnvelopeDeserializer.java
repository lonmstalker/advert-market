package com.advertmarket.shared.event;

import com.advertmarket.shared.json.JsonException;
import com.advertmarket.shared.json.JsonFacade;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Two-pass deserializer for {@link EventEnvelope}.
 *
 * <p>First reads the {@code eventType} field to resolve the
 * payload class from {@link EventTypeRegistry}, then uses
 * Jackson parametric type to deserialize the full envelope.
 */
@RequiredArgsConstructor
public class EventEnvelopeDeserializer {

    private final @NonNull JsonFacade json;
    private final @NonNull EventTypeRegistry registry;

    /**
     * Deserializes JSON bytes into an {@link EventEnvelope}.
     *
     * @param data JSON byte array
     * @return the deserialized envelope
     * @throws EventDeserializationException on failure
     */
    @NonNull
    public EventEnvelope<?> deserialize(byte @NonNull [] data) {
        try {
            return doParse(json.readTree(data));
        } catch (JsonException e) {
            throw new EventDeserializationException(
                    "Malformed JSON in event envelope", e);
        }
    }

    /**
     * Deserializes a JSON string into an {@link EventEnvelope}.
     *
     * @param jsonStr JSON string
     * @return the deserialized envelope
     * @throws EventDeserializationException on failure
     */
    @NonNull
    public EventEnvelope<?> deserialize(@NonNull String jsonStr) {
        try {
            return doParse(json.readTree(jsonStr));
        } catch (JsonException e) {
            throw new EventDeserializationException(
                    "Malformed JSON in event envelope", e);
        }
    }

    @SuppressWarnings("unchecked")
    private EventEnvelope<?> doParse(JsonNode root) {
        var eventTypeNode = root.get("eventType");
        if (eventTypeNode == null || eventTypeNode.isNull()) {
            throw new EventDeserializationException(
                    "Missing eventType field in envelope");
        }
        var eventType = eventTypeNode.asText();

        var payloadClass = registry.resolve(eventType);
        if (payloadClass == null) {
            throw new EventDeserializationException(
                    "Unknown event type: " + eventType);
        }

        try {
            var javaType = json.typeFactory()
                    .constructParametricType(
                            EventEnvelope.class, payloadClass);
            return json.convertValue(root, javaType);
        } catch (JsonException e) {
            throw new EventDeserializationException(
                    "Failed to deserialize payload for "
                            + eventType, e);
        }
    }
}
