package com.advertmarket.identity.web;

import com.advertmarket.identity.api.dto.OnboardingRequest;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.AuthService;
import com.advertmarket.identity.api.port.UserService;
import com.advertmarket.identity.security.TelegramAuthentication;
import com.advertmarket.shared.model.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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
@Tag(name = "Profile", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final UserService userService;
    private final AuthService authService;

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
    public UserProfile getProfile(
            @AuthenticationPrincipal UserId userId) {
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
    public UserProfile completeOnboarding(
            @AuthenticationPrincipal UserId userId,
            @Valid @RequestBody OnboardingRequest request) {
        return userService.completeOnboarding(userId, request);
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
            @AuthenticationPrincipal UserId userId) {
        userService.deleteAccount(userId);
        var auth = (TelegramAuthentication) SecurityContextHolder
                .getContext().getAuthentication();
        authService.logout(auth.getJti());
    }
}
