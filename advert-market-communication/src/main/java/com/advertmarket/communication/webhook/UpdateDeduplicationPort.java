package com.advertmarket.communication.webhook;

/**
 * Port for deduplicating Telegram updates by update_id.
 */
public interface UpdateDeduplicationPort {

    /**
     * Attempts to mark the given update_id as processed.
     *
     * @param updateId the Telegram update_id
     * @return true if this is the first time (not a duplicate)
     */
    boolean tryAcquire(int updateId);
}
