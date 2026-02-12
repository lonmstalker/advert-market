package com.advertmarket.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Response body for a successful login.
 *
 * @param accessToken JWT access token
 * @param expiresIn   token lifetime in seconds
 * @param user        summary of the authenticated user
 */
@Schema(description = "Successful login response with JWT and user info")
public record LoginResponse(
        @Schema(description = "JWT access token",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        @NonNull String accessToken,
        @Schema(description = "Token lifetime in seconds",
                example = "3600")
        long expiresIn,
        @Schema(description = "Summary of the authenticated user")
        @NonNull UserSummary user
) {

    /**
     * Compact user info returned with the token.
     *
     * @param id          Telegram user ID
     * @param username    Telegram username (may be empty)
     * @param displayName computed display name
     */
    @Schema(description = "Compact user info returned with the token")
    public record UserSummary(
            @Schema(description = "Telegram user ID",
                    example = "42")
            long id,
            @Schema(description = "Telegram username (without @)",
                    example = "johndoe")
            @NonNull String username,
            @Schema(description = "Display name from first + last name",
                    example = "John Doe")
            @NonNull String displayName
    ) {
    }
}
