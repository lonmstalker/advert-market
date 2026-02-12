package com.advertmarket.shared.event;

import com.advertmarket.shared.json.JsonFacade;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Serializes {@link EventEnvelope} instances to JSON.
 */
@RequiredArgsConstructor
public class EventEnvelopeSerializer {

    private final @NonNull JsonFacade json;

    /**
     * Serializes an envelope to JSON bytes.
     *
     * @param envelope the event envelope
     * @return JSON byte array
     */
    public byte @NonNull [] serialize(
            @NonNull EventEnvelope<?> envelope) {
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
        return json.toJson(envelope);
    }
}
