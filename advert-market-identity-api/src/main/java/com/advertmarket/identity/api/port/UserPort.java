package com.advertmarket.identity.api.port;

import com.advertmarket.identity.api.dto.OnboardingRequest;
import com.advertmarket.identity.api.dto.UpdateLanguageRequest;
import com.advertmarket.identity.api.dto.UpdateSettingsRequest;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.model.UserId;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * User profile operations.
 */
public interface UserPort {

    /**
     * Returns the profile for the given user.
     *
     * @param userId user identifier
     * @return user profile
     * @throws EntityNotFoundException if user not found
     */
    @NonNull
    UserProfile getProfile(@NonNull UserId userId);

    /**
     * Completes onboarding for the user.
     *
     * @param userId  user identifier
     * @param request onboarding data (interests)
     * @return updated user profile
     */
    @NonNull
    UserProfile completeOnboarding(
            @NonNull UserId userId,
            @NonNull OnboardingRequest request);

    /**
     * Updates the user's language.
     *
     * @param userId  user identifier
     * @param request language update data
     * @return updated user profile
     */
    @NonNull
    UserProfile updateLanguage(
            @NonNull UserId userId,
            @NonNull UpdateLanguageRequest request);

    /**
     * Updates the user's display settings (currency and/or notifications).
     *
     * @param userId  user identifier
     * @param request settings update data
     * @return updated user profile
     */
    @NonNull
    UserProfile updateSettings(
            @NonNull UserId userId,
            @NonNull UpdateSettingsRequest request);

    /**
     * Soft-deletes the user account and clears PII.
     *
     * @param userId user identifier
     */
    void deleteAccount(@NonNull UserId userId);
}
