package com.advertmarket.shared.audit;

/**
 * Actor type for audit log entries.
 */
public enum AuditActorType {

    /** Authenticated user action. */
    USER,

    /** System-initiated action (scheduler, timeout). */
    SYSTEM,

    /** Platform operator manual action. */
    OPERATOR
}
