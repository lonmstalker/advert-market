package com.advertmarket.financial.api.event;

/**
 * How a reconciliation process was triggered.
 */
public enum ReconciliationTriggerType {

    /** Triggered by a cron schedule. */
    SCHEDULED,

    /** Triggered manually by a platform operator. */
    MANUAL
}
