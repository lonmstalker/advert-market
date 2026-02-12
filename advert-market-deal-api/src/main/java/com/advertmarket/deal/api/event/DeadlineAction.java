package com.advertmarket.deal.api.event;

/**
 * Action to take when a deal deadline fires.
 */
public enum DeadlineAction {

    /** Expire the deal after timeout. */
    EXPIRE,

    /** Cancel the deal. */
    CANCEL
}
