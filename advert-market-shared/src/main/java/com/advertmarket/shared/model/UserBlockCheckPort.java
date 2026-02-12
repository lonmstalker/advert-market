package com.advertmarket.shared.model;

/**
 * Read-only projection for checking if a user is blocked.
 *
 * <p>Lives in shared because it is consumed by modules (identity, communication)
 * that do not depend on each other.
 */
public interface UserBlockCheckPort {

    /**
     * Returns {@code true} if the given user is currently blocked.
     *
     * @param userId the Telegram user id
     * @return true if the user is blocked
     */
    boolean isBlocked(long userId);
}
