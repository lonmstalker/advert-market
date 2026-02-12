package com.advertmarket.shared.exception;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Thrown when an invalid state transition is attempted.
 */
@Getter
public class InvalidStateTransitionException
        extends DomainException {

    private final @NonNull String entityType;
    private final @NonNull String from;
    private final @NonNull String to;

    /**
     * Creates an invalid-state-transition exception.
     *
     * @param entityType entity type (e.g., "Deal")
     * @param from current state
     * @param to attempted target state
     */
    public InvalidStateTransitionException(
            @NonNull String entityType,
            @NonNull String from,
            @NonNull String to) {
        super("INVALID_STATE_TRANSITION",
                String.format(
                        "Cannot transition %s from %s to %s",
                        entityType, from, to));
        this.entityType = entityType;
        this.from = from;
        this.to = to;
    }
}
