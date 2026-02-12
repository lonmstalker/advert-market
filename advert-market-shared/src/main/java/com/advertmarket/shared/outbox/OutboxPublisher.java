package com.advertmarket.shared.outbox;

import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for publishing outbox entries to a message broker.
 */
public interface OutboxPublisher {

    /**
     * Publishes an outbox entry to its target topic.
     *
     * @param entry the entry to publish
     * @return a future that completes when the message is acknowledged
     */
    @NonNull CompletableFuture<Void> publish(
            @NonNull OutboxEntry entry);
}
