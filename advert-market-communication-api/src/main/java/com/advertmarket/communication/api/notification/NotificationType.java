package com.advertmarket.communication.api.notification;

/**
 * Types of notifications sent to users via Telegram.
 */
public enum NotificationType {

    /** New advertising offer received. */
    NEW_OFFER,
    /** Offer accepted by the channel owner. */
    OFFER_ACCEPTED,
    /** Offer rejected by the channel owner. */
    OFFER_REJECTED,
    /** Escrow funded by the advertiser. */
    ESCROW_FUNDED,
    /** Creative material submitted for review. */
    CREATIVE_SUBMITTED,
    /** Creative approved by the channel owner. */
    CREATIVE_APPROVED,
    /** Revision requested for the creative. */
    REVISION_REQUESTED,
    /** Advertisement published on the channel. */
    PUBLISHED,
    /** Delivery verified by the system. */
    DELIVERY_VERIFIED,
    /** Payout sent to the channel owner. */
    PAYOUT_SENT,
    /** Dispute opened by a party. */
    DISPUTE_OPENED,
    /** Dispute resolved. */
    DISPUTE_RESOLVED,
    /** Deal expired without completion. */
    DEAL_EXPIRED,
    /** Deal cancelled by a party. */
    DEAL_CANCELLED,
    /** Financial reconciliation alert. */
    RECONCILIATION_ALERT
}
