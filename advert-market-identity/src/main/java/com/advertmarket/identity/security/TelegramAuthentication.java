package com.advertmarket.identity.security;

import com.advertmarket.shared.model.UserId;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authentication token for Telegram Mini App users.
 *
 * <p>Principal is a {@link UserId}. Carries the {@code isOperator} flag
 * for downstream authorization decisions.
 */
@Getter
public class TelegramAuthentication implements Authentication {

    private final @NonNull UserId userId;
    private final boolean operator;
    private final @NonNull String jti;

    /**
     * Creates a new authenticated token.
     *
     * @param userId     the authenticated user
     * @param isOperator whether the user is a platform operator
     * @param jti        the JWT unique identifier
     */
    public TelegramAuthentication(
            @NonNull UserId userId,
            boolean isOperator,
            @NonNull String jti) {
        this.userId = userId;
        this.operator = isOperator;
        this.jti = jti;
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public @NonNull UserId getPrincipal() {
        return userId;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        throw new UnsupportedOperationException(
                "TelegramAuthentication is always authenticated");
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
