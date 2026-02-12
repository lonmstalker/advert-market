package com.advertmarket.identity.api.port;

import java.time.Instant;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for JWT token blacklist operations.
 */
public interface TokenBlacklistPort {

    /**
     * Blacklists a JWT by its unique identifier.
     *
     * @param jti       the JWT ID
     * @param expiresAt the token expiration time (used for TTL)
     */
    void blacklist(@NonNull String jti, @NonNull Instant expiresAt);

    /**
     * Checks whether a JWT is blacklisted.
     *
     * @param jti the JWT ID
     * @return true if the token is blacklisted
     */
    boolean isBlacklisted(@NonNull String jti);
}
