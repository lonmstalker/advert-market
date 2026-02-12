package com.advertmarket.shared.security;

import com.advertmarket.shared.model.UserId;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.security.core.Authentication;

/**
 * Minimal contract for extracting principal identity from the
 * security context.
 *
 * <p>Implementations live in domain modules (e.g. identity)
 * and must be registered as the {@link Authentication} in
 * {@link org.springframework.security.core.context.SecurityContextHolder}.
 *
 * <p>Implementations must ensure that {@link #isAuthenticated()}
 * returns {@code true} when set in the security context.
 * {@link SecurityContextUtil} rejects unauthenticated instances.
 */
public interface PrincipalAuthentication extends Authentication {

    /** Returns the authenticated user identifier. */
    @NonNull UserId getUserId();

    /** Returns the JWT token identifier (jti claim). */
    @NonNull String getJti();

    /** Returns {@code true} if the user is an operator. */
    boolean isOperator();
}
