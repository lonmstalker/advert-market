package com.advertmarket.shared.exception;

import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base exception for domain-level errors.
 */
@Getter
public class DomainException extends RuntimeException {

    private final @NonNull String errorCode;
    private final @Nullable Map<String, Object> context;

    /**
     * Creates a domain exception.
     *
     * @param errorCode machine-readable error code
     * @param message human-readable message
     */
    public DomainException(
            @NonNull String errorCode,
            @NonNull String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(
                errorCode, "errorCode");
        this.context = null;
    }

    /**
     * Creates a domain exception with context.
     *
     * @param errorCode machine-readable error code
     * @param message human-readable message
     * @param context structured error context
     */
    public DomainException(
            @NonNull String errorCode,
            @NonNull String message,
            @Nullable Map<String, Object> context) {
        super(message);
        this.errorCode = Objects.requireNonNull(
                errorCode, "errorCode");
        this.context = context != null
                ? Map.copyOf(context) : null;
    }

    /**
     * Creates a domain exception with a cause.
     *
     * @param errorCode machine-readable error code
     * @param message human-readable message
     * @param cause the underlying cause
     */
    public DomainException(
            @NonNull String errorCode,
            @NonNull String message,
            @NonNull Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(
                errorCode, "errorCode");
        this.context = null;
    }
}
