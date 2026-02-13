package com.advertmarket.app.internal;

import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Response for the worker callback HTTP endpoint.
 *
 * @param eventId generated event identifier
 * @param correlationId correlation identifier from the request
 * @param status processing status
 */
public record WorkerCallbackResponse(
        @NonNull UUID eventId,
        @NonNull UUID correlationId,
        @NonNull String status) {
}
