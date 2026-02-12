package com.advertmarket.shared.audit;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for recording audit log entries.
 *
 * <p>Implementations may write to a database table, Kafka topic,
 * or external audit service.
 */
public interface AuditPort {

    /**
     * Records an audit entry.
     *
     * @param entry the audit entry to record
     */
    void record(@NonNull AuditEntry entry);
}
