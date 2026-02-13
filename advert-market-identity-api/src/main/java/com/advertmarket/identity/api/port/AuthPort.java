package com.advertmarket.identity.api.port;

import com.advertmarket.identity.api.dto.LoginRequest;
import com.advertmarket.identity.api.dto.LoginResponse;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Authenticates users via Telegram initData.
 */
public interface AuthPort {

    /**
     * Validates Telegram initData and issues a JWT.
     *
     * @param request login request containing raw initData
     * @return login response with access token and user summary
     */
    @NonNull
    LoginResponse login(@NonNull LoginRequest request);

    /**
     * Logs out a user by blacklisting the current JWT.
     *
     * @param jti             the JWT unique identifier to revoke
     * @param tokenExpSeconds token expiration as epoch seconds
     */
    void logout(@NonNull String jti, long tokenExpSeconds);
}
