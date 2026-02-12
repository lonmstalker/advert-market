package com.advertmarket.shared.audit;

import com.advertmarket.shared.event.DomainEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Domain event for audit log entries published to Kafka.
 *
 * @param actorType type of actor performing the action
 * @param actorId actor identifier (null for system actions)
 * @param action action performed
 * @param entityType type of the affected entity
 * @param entityId identifier of the affected entity
 */
public record AuditEvent(
        @NonNull AuditActorType actorType,
        @Nullable String actorId,
        @NonNull String action,
        @NonNull String entityType,
        @NonNull String entityId) implements DomainEvent {
}
