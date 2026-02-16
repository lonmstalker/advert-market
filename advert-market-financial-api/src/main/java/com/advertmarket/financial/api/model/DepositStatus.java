package com.advertmarket.financial.api.model;

/**
 * Deposit processing status exposed to deal/frontend flows.
 */
public enum DepositStatus {

    /** Waiting for initial transfer to the escrow address. */
    AWAITING_PAYMENT,

    /** Matching transfer detected on chain, awaiting confirmations. */
    TX_DETECTED,

    /** Confirmations are being accumulated. */
    CONFIRMING,

    /** Confirmed on-chain but requires operator approval. */
    AWAITING_OPERATOR_REVIEW,

    /** Deposit accepted and confirmed. */
    CONFIRMED,

    /** Deposit window expired without valid completion. */
    EXPIRED,

    /** Received amount is below expected. */
    UNDERPAID,

    /** Received amount is above expected. */
    OVERPAID,

    /** Deposit explicitly rejected by operator/system. */
    REJECTED
}
