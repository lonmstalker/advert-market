package com.advertmarket.delivery.api.event;

/**
 * Reasons for a delivery verification failure.
 */
public enum DeliveryFailureReason {

    /** The published post was deleted from the channel. */
    POST_DELETED,

    /** Post content was modified after publication. */
    CONTENT_MODIFIED,

    /** The channel became inaccessible to the bot. */
    CHANNEL_INACCESSIBLE
}
