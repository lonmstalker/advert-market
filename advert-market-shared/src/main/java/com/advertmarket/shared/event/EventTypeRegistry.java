package com.advertmarket.shared.event;

import com.advertmarket.shared.FenumGroup;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Mutable registry mapping event type strings to payload classes.
 *
 * <p>Populated at application startup via {@code @Configuration}.
 * Used by {@link EventEnvelopeDeserializer} for two-pass
 * deserialization.
 */
@SuppressWarnings("fenum:argument")
public class EventTypeRegistry {

    private final Map<String, Class<? extends DomainEvent>> types =
            new ConcurrentHashMap<>();

    /**
     * Registers a mapping from event type to payload class.
     *
     * @param eventType the event type discriminator
     * @param payloadClass the concrete payload class
     */
    public void register(
            @Fenum(FenumGroup.EVENT_TYPE) @NonNull String eventType,
            @NonNull Class<? extends DomainEvent> payloadClass) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(payloadClass, "payloadClass");
        var existing = types.putIfAbsent(
                eventType, payloadClass);
        if (existing != null
                && !existing.equals(payloadClass)) {
            throw new IllegalStateException(
                    "Duplicate event type registration: "
                            + eventType + " already mapped to "
                            + existing.getName());
        }
    }

    /**
     * Resolves a payload class by event type.
     *
     * @param eventType the event type discriminator
     * @return the payload class, or {@code null} if unknown
     */
    public @Nullable Class<? extends DomainEvent> resolve(
            @Fenum(FenumGroup.EVENT_TYPE) @NonNull String eventType) {
        Objects.requireNonNull(eventType, "eventType");
        return types.get(eventType);
    }

    /** Returns the number of registered event types. */
    public int size() {
        return types.size();
    }
}
