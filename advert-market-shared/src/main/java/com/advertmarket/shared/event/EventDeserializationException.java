package com.advertmarket.shared.event;

import com.advertmarket.shared.exception.DomainException;

/**
 * Non-retryable exception for event deserialization failures.
 *
 * <p>Thrown when an event envelope cannot be deserialized due to
 * unknown event type or malformed JSON. These errors should be
 * routed to the dead-letter topic without retries.
 */
public class EventDeserializationException extends DomainException {

    /**
     * Creates a deserialization exception.
     *
     * @param message description of the deserialization failure
     */
    public EventDeserializationException(String message) {
        super("EVENT_DESERIALIZATION_ERROR", message);
    }

    /**
     * Creates a deserialization exception with a cause.
     *
     * @param message description of the deserialization failure
     * @param cause the underlying cause
     */
    public EventDeserializationException(String message,
            Throwable cause) {
        super("EVENT_DESERIALIZATION_ERROR", message, cause);
    }
}
