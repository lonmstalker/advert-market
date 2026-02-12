package com.advertmarket.shared.audit;

import lombok.Builder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Immutable audit log entry for recording state changes.
 *
 * @param actorType type of actor performing the action
 * @param actorId actor identifier (null for system actions)
 * @param action action performed (e.g. "CREATE_DEAL", "APPROVE_CREATIVE")
 * @param entityType type of the affected entity (e.g. "DEAL", "CHANNEL")
 * @param entityId identifier of the affected entity
 * @param oldValue previous state as JSON (null for create operations)
 * @param newValue new state as JSON (null for delete operations)
 * @param ipAddress client IP address (null for system actions)
 */
@Builder
public record AuditEntry(
        @NonNull AuditActorType actorType,
        @Nullable String actorId,
        @NonNull String action,
        @NonNull String entityType,
        @NonNull String entityId,
        @Nullable String oldValue,
        @Nullable String newValue,
        @Nullable String ipAddress) {

    /** Validates required fields. */
    public AuditEntry {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "action must not be blank");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException(
                    "entityType must not be blank");
        }
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException(
                    "entityId must not be blank");
        }
        if (actorType == null) {
            throw new IllegalArgumentException(
                    "actorType must not be null");
        }
    }
}
