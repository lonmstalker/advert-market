package com.advertmarket.shared.model;

/**
 * Actor types in the marketplace.
 */
public enum ActorType {

    /** Advertiser who creates deals. */
    ADVERTISER,

    /** Channel owner who fulfills deals. */
    CHANNEL_OWNER,

    /** Admin of a channel with delegated permissions. */
    CHANNEL_ADMIN,

    /** Platform operator for manual actions. */
    PLATFORM_OPERATOR,

    /** System-initiated actions (timeouts, scheduled). */
    SYSTEM
}
