package com.advertmarket.shared.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Deal lifecycle states.
 *
 * <p>See state machine diagram for valid transitions.
 * Transition logic resides in the deal module.
 */
public enum DealStatus {

    /** Deal created, not yet submitted. */
    DRAFT,

    /** Offer sent to channel owner. */
    OFFER_PENDING,

    /** Parties negotiating terms. */
    NEGOTIATING,

    /** Terms agreed, awaiting payment setup. */
    ACCEPTED,

    /** Deposit address generated, waiting for TON. */
    AWAITING_PAYMENT,

    /** Escrow funded, creative workflow begins. */
    FUNDED,

    /** Creative draft submitted for review. */
    CREATIVE_SUBMITTED,

    /** Creative approved by advertiser. */
    CREATIVE_APPROVED,

    /** Publication scheduled at specific time. */
    SCHEDULED,

    /** Creative published to channel. */
    PUBLISHED,

    /** Retention verification in progress (24h). */
    DELIVERY_VERIFYING,

    /** Delivery verified, payout sent. */
    COMPLETED_RELEASED,

    /** Dispute opened, under review. */
    DISPUTED,

    /** Deal cancelled. */
    CANCELLED,

    /** Escrow refunded after dispute. */
    REFUNDED,

    /** Partial refund after dispute (time-based split). */
    PARTIALLY_REFUNDED,

    /** Deal expired due to timeout. */
    EXPIRED;

    private static final Set<DealStatus> TERMINAL = EnumSet.of(
            COMPLETED_RELEASED, CANCELLED, REFUNDED,
            PARTIALLY_REFUNDED, EXPIRED);

    private static final Set<DealStatus> FUNDED_STATES = EnumSet.of(
            FUNDED, CREATIVE_SUBMITTED, CREATIVE_APPROVED,
            SCHEDULED, PUBLISHED, DELIVERY_VERIFYING,
            DISPUTED);

    /** Returns {@code true} if this is a terminal state. */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /**
     * Returns {@code true} if escrow is funded in this state.
     */
    public boolean isFunded() {
        return FUNDED_STATES.contains(this);
    }

    /**
     * Returns {@code true} if cancellation from this state
     * requires a refund of escrowed funds.
     */
    public boolean requiresRefundOnCancel() {
        return FUNDED_STATES.contains(this);
    }
}
