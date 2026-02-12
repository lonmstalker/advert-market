package com.advertmarket.shared.outbox;

/**
 * Status of an outbox entry.
 */
public enum OutboxStatus {

    /** Waiting to be published. */
    PENDING,

    /** Currently being processed by the poller. */
    PROCESSING,

    /** Successfully delivered to Kafka. */
    DELIVERED,

    /** Failed after exhausting all retries. */
    FAILED
}
