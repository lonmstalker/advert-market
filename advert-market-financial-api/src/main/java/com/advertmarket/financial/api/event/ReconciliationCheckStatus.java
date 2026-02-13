package com.advertmarket.financial.api.event;

/**
 * Status of a single reconciliation check.
 */
public enum ReconciliationCheckStatus {

    /** Check passed successfully. */
    PASS,

    /** Check detected a discrepancy. */
    FAIL
}
