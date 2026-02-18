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
    RECONCILIATION_ALERT,
    /** Bot was removed from a channel. */
    CHANNEL_BOT_REMOVED,
    /** Bot was demoted in a channel (lost admin rights). */
    CHANNEL_BOT_DEMOTED,
    /** Channel owner lost admin access and listing was deactivated. */
    CHANNEL_OWNERSHIP_LOST,
    /** Bot admin rights were restored and channel reactivated. */
    CHANNEL_BOT_RESTORED,
    /** Unclaimed payout reminder sent to channel owner. */
    PAYOUT_UNCLAIMED,
    /** 30+ day unclaimed payout escalated to operators. */
    PAYOUT_UNCLAIMED_OPERATOR_REVIEW
}
