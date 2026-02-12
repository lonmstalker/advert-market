package com.advertmarket.shared.json;

import com.advertmarket.shared.exception.DomainException;

/**
 * Unchecked exception for JSON serialization/deserialization errors.
 *
 * <p>Wraps checked {@link java.io.IOException} from Jackson into
 * an unchecked {@link DomainException} with error code
 * {@code JSON_ERROR}.
 */
public class JsonException extends DomainException {

    /**
     * Creates a JSON exception.
     *
     * @param message description of the failure
     */
    public JsonException(String message) {
        super("JSON_ERROR", message);
    }

    /**
     * Creates a JSON exception with a cause.
     *
     * @param message description of the failure
     * @param cause the underlying cause
     */
    public JsonException(String message, Throwable cause) {
        super("JSON_ERROR", message, cause);
    }
}
