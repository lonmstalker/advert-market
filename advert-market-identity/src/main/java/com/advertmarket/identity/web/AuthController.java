package com.advertmarket.identity.web;

import com.advertmarket.identity.api.dto.LoginRequest;
import com.advertmarket.identity.api.dto.LoginResponse;
import com.advertmarket.identity.api.port.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
