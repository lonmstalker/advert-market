package com.advertmarket.shared.exception;

import java.util.Locale;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Thrown when a requested entity is not found.
 */
@Getter
public class EntityNotFoundException extends DomainException {

    private final @NonNull String entityType;
    private final @NonNull String entityId;

    /**
     * Creates an entity-not-found exception.
     *
     * @param entityType the type of entity (e.g., "Deal")
     * @param entityId the entity identifier
     */
    public EntityNotFoundException(
            @NonNull String entityType,
            @NonNull String entityId) {
        super(entityType.toUpperCase(Locale.ROOT)
                        + "_NOT_FOUND",
                entityType + " not found: " + entityId);
        this.entityType = entityType;
        this.entityId = entityId;
    }
}
