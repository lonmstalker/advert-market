package com.advertmarket.identity.web;

import com.advertmarket.identity.api.dto.OnboardingRequest;
import com.advertmarket.identity.api.dto.UpdateLanguageRequest;
import com.advertmarket.identity.api.dto.UpdateSettingsRequest;
import com.advertmarket.identity.api.dto.UpdateTonAddressRequest;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.AuthPort;
import com.advertmarket.identity.api.port.UserPort;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.security.PrincipalAuthentication;
import com.advertmarket.shared.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * User profile endpoints.
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Profile", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final UserPort userService;
    private final AuthPort authService;

    /**
     * Returns the profile of the authenticated user.
     */
    @GetMapping
    @Operation(summary = "Get current user profile")
    @ApiResponse(
            responseCode = "200",
            description = "Profile retrieved",
            content = @Content(schema =
            @Schema(implementation = UserProfile.class))
    )
    @ApiResponse(responseCode = "401",
            description = "Not authenticated")
    public @NonNull UserProfile getProfile(
            @AuthenticationPrincipal @NonNull UserId userId) {
        return userService.getProfile(userId);
    }

    /**
     * Completes onboarding for the authenticated user.
     */
    @PutMapping("/onboarding")
    @Operation(summary = "Complete user onboarding")
    @ApiResponse(
            responseCode = "200",
            description = "Onboarding completed",
            content = @Content(schema =
            @Schema(implementation = UserProfile.class))
    )
    @ApiResponse(responseCode = "400",
            description = "Invalid interests")
    @ApiResponse(responseCode = "401",
            description = "Not authenticated")
    public @NonNull UserProfile completeOnboarding(
            @AuthenticationPrincipal @NonNull UserId userId,
            @RequestBody @Valid OnboardingRequest request) {
        return userService.completeOnboarding(userId, request);
    }

    /**
     * Updates the authenticated user's language.
     */
    @PutMapping("/language")
    @Operation(summary = "Update user language")
    @ApiResponse(
            responseCode = "200",
            description = "Language updated",
            content = @Content(schema =
            @Schema(implementation = UserProfile.class))
    )
    @ApiResponse(responseCode = "400",
            description = "Invalid language code")
    @ApiResponse(responseCode = "401",
            description = "Not authenticated")
    public @NonNull UserProfile updateLanguage(
            @AuthenticationPrincipal @NonNull UserId userId,
            @RequestBody @Valid UpdateLanguageRequest request) {
        return userService.updateLanguage(userId, request);
    }

    /**
     * Updates the authenticated user's settings (currency, notifications).
     */
    @PutMapping("/settings")
    @Operation(summary = "Update user settings")
    @ApiResponse(
            responseCode = "200",
            description = "Settings updated",
            content = @Content(schema =
            @Schema(implementation = UserProfile.class))
    )
    @ApiResponse(responseCode = "400",
            description = "Invalid settings")
    @ApiResponse(responseCode = "401",
            description = "Not authenticated")
    public @NonNull UserProfile updateSettings(
            @AuthenticationPrincipal @NonNull UserId userId,
            @RequestBody @Valid UpdateSettingsRequest request) {
        return userService.updateSettings(userId, request);
    }

    /**
     * Updates the authenticated user's TON wallet address.
     */
    @PutMapping("/wallet")
    @Operation(summary = "Update user TON wallet address")
    @ApiResponse(
            responseCode = "200",
            description = "Wallet address updated",
            content = @Content(schema =
            @Schema(implementation = UserProfile.class))
    )
    @ApiResponse(responseCode = "400",
            description = "Invalid TON address format")
    @ApiResponse(responseCode = "401",
            description = "Not authenticated")
    public @NonNull UserProfile updateWallet(
            @AuthenticationPrincipal @NonNull UserId userId,
            @RequestBody @Valid UpdateTonAddressRequest request) {
        return userService.updateTonAddress(userId, request);
    }

    /**
     * Soft-deletes the user account and blacklists the current JWT.
     */
    @DeleteMapping
    @Operation(summary = "Delete user account (soft delete)")
    @ApiResponse(responseCode = "204",
            description = "Account deleted")
    @ApiResponse(responseCode = "401",
            description = "Not authenticated")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            @AuthenticationPrincipal @NonNull UserId userId) {
        userService.deleteAccount(userId);
        PrincipalAuthentication auth =
                SecurityContextUtil.currentAuthentication();
        authService.logout(auth.getJti(),
                auth.getTokenExpSeconds());
    }
}
