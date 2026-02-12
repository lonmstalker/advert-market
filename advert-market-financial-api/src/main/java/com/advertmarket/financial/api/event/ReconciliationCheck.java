package com.advertmarket.financial.api.event;

/**
 * Types of reconciliation checks.
 */
public enum ReconciliationCheck {

    /** Verify ledger account balances sum to zero. */
    LEDGER_BALANCE,

    /** Compare ledger with TON blockchain state. */
    LEDGER_VS_TON,

    /** Compare ledger with deal amounts. */
    LEDGER_VS_DEALS,

    /** Verify CQRS projection consistency. */
    CQRS_PROJECTION
}
