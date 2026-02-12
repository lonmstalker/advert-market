package com.advertmarket.shared.model;

/**
 * Ledger entry types for the double-entry bookkeeping system.
 */
public enum EntryType {

    /** Advertiser deposits TON for a deal. */
    ESCROW_DEPOSIT,

    /** Partial deposit received (underpayment). */
    PARTIAL_DEPOSIT,

    /** Promote partial deposits to escrow. */
    PARTIAL_DEPOSIT_PROMOTE,

    /** Full escrow released after delivery verification. */
    ESCROW_RELEASE,

    /** Owner's share credited from escrow release. */
    OWNER_PAYOUT,

    /** Commission credited from escrow release. */
    PLATFORM_COMMISSION,

    /** Full refund to advertiser. */
    ESCROW_REFUND,

    /** Partial refund (time-based split). */
    PARTIAL_REFUND,

    /** Refund of overpaid amount. */
    OVERPAYMENT_REFUND,

    /** Auto-refund of deposit to expired deal. */
    LATE_DEPOSIT_REFUND,

    /** Move per-deal commission to treasury. */
    COMMISSION_SWEEP,

    /** Owner withdraws to TON address. */
    OWNER_WITHDRAWAL,

    /** TON gas fee recording. */
    NETWORK_FEE,

    /** Gas fee deducted from refund amount. */
    NETWORK_FEE_REFUND,

    /** Operator-initiated reversal. */
    REVERSAL,

    /** Operator-initiated fee correction. */
    FEE_ADJUSTMENT,

    /** Monthly write-off of subwallet dust. */
    DUST_WRITEOFF
}
