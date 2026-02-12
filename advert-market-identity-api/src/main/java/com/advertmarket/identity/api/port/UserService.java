package com.advertmarket.identity.api.port;

import com.advertmarket.identity.api.dto.OnboardingRequest;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.shared.model.UserId;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * User profile operations.
 */
public interface UserService {

    /**
     * Returns the profile for the given user.
     *
     * @param userId user identifier
     * @return user profile
     * @throws com.advertmarket.shared.exception.EntityNotFoundException
     *         if user not found
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
     * Soft-deletes the user account and clears PII.
     *
     * @param userId user identifier
     */
    void deleteAccount(@NonNull UserId userId);
}
