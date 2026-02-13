package com.advertmarket.app.filter;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authentication token for internal service requests.
 *
 * <p>Grants {@code ROLE_INTERNAL} authority.
 */
public class InternalServiceAuthentication
        extends AbstractAuthenticationToken {

    /**
     * Creates an authenticated internal service token.
     */
    public InternalServiceAuthentication() {
        super(List.of(
                new SimpleGrantedAuthority("ROLE_INTERNAL")));
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return "internal-service";
    }
}
