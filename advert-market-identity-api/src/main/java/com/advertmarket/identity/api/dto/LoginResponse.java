package com.advertmarket.identity.api.dto;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Response body for a successful login.
 *
 * @param accessToken JWT access token
 * @param expiresIn   token lifetime in seconds
 * @param user        summary of the authenticated user
 */
public record LoginResponse(
        @NonNull String accessToken,
        long expiresIn,
        @NonNull UserSummary user
) {

    /**
     * Compact user info returned with the token.
     *
     * @param id          Telegram user ID
     * @param username    Telegram username (may be empty)
     * @param displayName computed display name
     */
    public record UserSummary(
            long id,
            @NonNull String username,
            @NonNull String displayName
    ) {
    }
}
