package com.advertmarket.shared.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Financial account types in the double-entry ledger.
 */
public enum AccountType {

    /** Platform's accumulated commission revenue. */
    PLATFORM_TREASURY,

    /** Per-deal escrow holding advertiser's funds. */
    ESCROW,

    /** Channel owner's pending payout balance. */
    OWNER_PENDING,

    /** Per-deal commission tracking. */
    COMMISSION,

    /** Virtual account representing external TON blockchain. */
    EXTERNAL_TON,

    /** Cumulative TON gas fees. */
    NETWORK_FEES,

    /** Excess funds when advertiser overpays. */
    OVERPAYMENT,

    /** Partial deposits before deal is fully funded. */
    PARTIAL_DEPOSIT,

    /** Temporary holding for deposits to expired deals. */
    LATE_DEPOSIT,

    /** Monthly write-off of subwallet dust. */
    DUST_WRITEOFF;

    private static final Set<AccountType> SINGLETONS = EnumSet.of(
            PLATFORM_TREASURY, EXTERNAL_TON,
            NETWORK_FEES, DUST_WRITEOFF);

    private static final Set<AccountType> ALLOW_NEGATIVE =
            EnumSet.of(EXTERNAL_TON, NETWORK_FEES, DUST_WRITEOFF);

    /** Returns {@code true} if this account type is a singleton. */
    public boolean isSingleton() {
        return SINGLETONS.contains(this);
    }

    /**
     * Returns {@code true} if this account type requires
     * non-negative balance at application level.
     */
    public boolean requiresNonNegativeBalance() {
        return !ALLOW_NEGATIVE.contains(this);
    }
}
