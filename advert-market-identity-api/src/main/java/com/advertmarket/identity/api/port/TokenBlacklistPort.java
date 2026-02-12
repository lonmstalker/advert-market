package com.advertmarket.identity.api.port;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for JWT token blacklisting (revocation).
 */
public interface TokenBlacklistPort {

    /**
     * Returns {@code true} if the given JTI has been blacklisted.
     *
     * @param jti the JWT unique identifier
     * @return true if the token is revoked
     */
    boolean isBlacklisted(@NonNull String jti);

    /**
     * Blacklists a JWT by its JTI.
     *
     * @param jti        the JWT unique identifier
     * @param ttlSeconds time-to-live in seconds (matches token lifetime)
     */
    void blacklist(@NonNull String jti, long ttlSeconds);
}
