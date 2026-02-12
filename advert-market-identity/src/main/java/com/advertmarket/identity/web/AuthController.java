package com.advertmarket.identity.web;

import com.advertmarket.identity.api.dto.LoginRequest;
import com.advertmarket.identity.api.dto.LoginResponse;
import com.advertmarket.identity.api.port.AuthService;
import com.advertmarket.identity.api.port.LoginRateLimiterPort;
import com.advertmarket.identity.security.TelegramAuthentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Telegram Mini App authentication")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiterPort loginRateLimiter;

    /**
     * Authenticates a user via Telegram initData and returns a JWT.
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login via Telegram initData",
            description = "Validates Telegram Mini App initData"
                    + " using HMAC-SHA256 and issues a JWT token"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(schema =
            @Schema(implementation = LoginResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request body"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired initData"
    )
    @ApiResponse(
            responseCode = "429",
            description = "Too many login attempts"
    )
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        loginRateLimiter.checkRate(httpRequest.getRemoteAddr());
        return authService.login(request);
    }

    /**
     * Logs out the current user by blacklisting the JWT.
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Logout and revoke current JWT",
            description = "Blacklists the current JWT token"
                    + " so it can no longer be used"
    )
    @ApiResponse(responseCode = "204",
            description = "Logged out successfully")
    @ApiResponse(responseCode = "401",
            description = "Not authenticated")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        var auth = (TelegramAuthentication) SecurityContextHolder
                .getContext().getAuthentication();
        authService.logout(auth.getJti());
    }
}
