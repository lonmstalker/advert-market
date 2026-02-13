package com.advertmarket.identity.security;

import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.security.PrincipalAuthentication;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authentication token for Telegram Mini App users.
 *
 * <p>Principal is a {@link UserId}. Carries the {@code isOperator} flag
 * for downstream authorization decisions.
 */
@Getter
@RequiredArgsConstructor
public class TelegramAuthentication implements PrincipalAuthentication {

    private final @NonNull UserId userId;
    private final boolean operator;
    private final @NonNull String jti;
    private final long tokenExpSeconds;

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public @Nullable Object getCredentials() {
        return null;
    }

    @Override
    public @Nullable Object getDetails() {
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
    public @NonNull String getName() {
        return userId.toString();
    }
}
