package com.advertmarket.shared.event;

import com.advertmarket.shared.json.JsonFacade;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Serializes {@link EventEnvelope} instances to JSON.
 */
public class EventEnvelopeSerializer {

    private final JsonFacade json;

    /**
     * Creates a serializer with the given JSON facade.
     *
     * @param json JSON facade
     */
    public EventEnvelopeSerializer(@NonNull JsonFacade json) {
        this.json = Objects.requireNonNull(json, "json");
    }

    /**
     * Serializes an envelope to JSON bytes.
     *
     * @param envelope the event envelope
     * @return JSON byte array
     */
    public byte[] serialize(
            @NonNull EventEnvelope<?> envelope) {
        Objects.requireNonNull(envelope, "envelope");
        return json.toBytes(envelope);
    }

    /**
     * Serializes an envelope to a JSON string.
     *
     * @param envelope the event envelope
     * @return JSON string
     */
    @NonNull
    public String serializeToString(
            @NonNull EventEnvelope<?> envelope) {
        Objects.requireNonNull(envelope, "envelope");
        return json.toJson(envelope);
    }
}
