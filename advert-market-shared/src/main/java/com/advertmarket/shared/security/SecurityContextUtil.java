package com.advertmarket.shared.security;

import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.model.UserId;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility for extracting the current principal from the
 * Spring Security context.
 */
public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    /**
     * Returns the current {@link PrincipalAuthentication}.
     *
     * @throws DomainException if no authentication is present,
     *     it is not a {@link PrincipalAuthentication},
     *     or it is not authenticated
     */
    public static @NonNull PrincipalAuthentication currentAuthentication() {
        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();
        if (auth instanceof PrincipalAuthentication principal
                && principal.isAuthenticated()) {
            return principal;
        }
        throw new DomainException(
                ErrorCodes.AUTH_INVALID_TOKEN, "Not authenticated");
    }

    /** Returns the current authenticated user identifier. */
    public static @NonNull UserId currentUserId() {
        return currentAuthentication().getUserId();
    }

    /** Returns the JWT token identifier of the current session. */
    public static @NonNull String currentJti() {
        return currentAuthentication().getJti();
    }

    /** Returns {@code true} if the current user is an operator. */
    public static boolean isOperator() {
        return currentAuthentication().isOperator();
    }
}
