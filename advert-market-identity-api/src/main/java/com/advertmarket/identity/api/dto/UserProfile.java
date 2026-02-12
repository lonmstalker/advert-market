package com.advertmarket.identity.api.dto;

import java.time.Instant;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Full user profile.
 *
 * @param id                  user identifier (same as telegramId)
 * @param telegramId          Telegram user ID
 * @param username            Telegram username
 * @param displayName         computed from first + last name
 * @param languageCode        IETF language tag
 * @param onboardingCompleted whether onboarding is done
 * @param interests           selected interest tags
 * @param createdAt           registration timestamp
 */
public record UserProfile(
        long id,
        long telegramId,
        @NonNull String username,
        @NonNull String displayName,
        @NonNull String languageCode,
        boolean onboardingCompleted,
        @NonNull List<String> interests,
        @NonNull Instant createdAt
) {

    /** Defensive copy constructor. */
    public UserProfile {
        interests = List.copyOf(interests);
    }
}
