package com.advertmarket.shared.exception;

import com.advertmarket.shared.FenumGroup;
import lombok.Getter;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Thrown when a requested entity is not found.
 */
@Getter
public class EntityNotFoundException extends DomainException {

    private final @NonNull String entityType;
    private final @NonNull String entityId;

    /**
     * Creates an entity-not-found exception with an explicit error code.
     *
     * @param errorCode catalog error code (e.g., ErrorCodes.DEAL_NOT_FOUND)
     * @param entityType the type of entity (e.g., "Deal")
     * @param entityId the entity identifier
     */
    public EntityNotFoundException(
            @Fenum(FenumGroup.ERROR_CODE) @NonNull String errorCode,
            @NonNull String entityType,
            @NonNull String entityId) {
        super(errorCode,
                entityType + " not found: " + entityId);
        this.entityType = entityType;
        this.entityId = entityId;
    }
}
