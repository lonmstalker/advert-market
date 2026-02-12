package com.advertmarket.identity.security;

import com.advertmarket.shared.security.SecurityContextUtil;
import org.springframework.stereotype.Component;

/**
 * ABAC authorization bean for use in SpEL expressions.
 *
 * <p>Usage: {@code @PreAuthorize("@auth.isOperator()")}.
 */
@Component("auth")
public class AuthorizationService {

    /** Returns {@code true} if the current user is a platform operator. */
    public boolean isOperator() {
        return SecurityContextUtil.isOperator();
    }
}
