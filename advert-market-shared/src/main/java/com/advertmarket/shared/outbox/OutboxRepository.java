package com.advertmarket.shared.outbox;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for outbox persistence operations.
 *
 * <p>Implementations should use {@code FOR UPDATE SKIP LOCKED}
 * when fetching pending entries to allow concurrent pollers.
 */
public interface OutboxRepository {

    /**
     * Persists a new outbox entry.
     *
     * @param entry the entry to persist
     */
    void save(@NonNull OutboxEntry entry);

    /**
     * Fetches a batch of pending entries ready for publishing.
     *
     * @param batchSize maximum number of entries to return
     * @return list of pending entries
     */
    @NonNull List<OutboxEntry> findPendingBatch(int batchSize);

    /**
     * Marks an entry as successfully delivered.
     *
     * @param id the entry identifier
     */
    void markDelivered(long id);

    /**
     * Marks an entry as permanently failed.
     *
     * @param id the entry identifier
     */
    void markFailed(long id);

    /**
     * Increments the retry count for a failed entry.
     *
     * @param id the entry identifier
     */
    void incrementRetry(long id);
}
