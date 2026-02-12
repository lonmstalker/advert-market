package com.advertmarket.financial.api.event;

/**
 * Reasons for a deposit failure.
 */
public enum DepositFailureReason {

    /** Received amount does not match expected. */
    AMOUNT_MISMATCH,

    /** Deposit was not received within the time window. */
    TIMEOUT,

    /** Deposit was rejected by the system. */
    REJECTED
}
