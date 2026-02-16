package com.advertmarket.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Full user profile.
 *
 * @param id                    user identifier (Telegram user ID)
 * @param username              Telegram username
 * @param displayName           computed from first + last name
 * @param languageCode          IETF language tag
 * @param displayCurrency       fiat currency for display
 * @param currencyMode          currency selection mode
 * @param notificationSettings  notification preferences
 * @param onboardingCompleted   whether onboarding is done
 * @param interests             selected interest tags
 * @param tonAddress            TON wallet address (non-bounceable raw format)
 * @param createdAt             registration timestamp
 */
@Schema(description = "Full user profile")
public record UserProfile(
        @Schema(description = "Telegram user ID", example = "42")
        long id,
        @Schema(description = "Telegram username", example = "johndoe")
        @NonNull String username,
        @Schema(description = "Display name", example = "John Doe")
        @NonNull String displayName,
        @Schema(description = "IETF language tag", example = "en")
        @NonNull String languageCode,
        @Schema(description = "Fiat display currency", example = "USD")
        @NonNull String displayCurrency,
        @Schema(description = "Currency mode", example = "AUTO")
        @NonNull CurrencyMode currencyMode,
        @Schema(description = "Notification preferences")
        @NonNull NotificationSettings notificationSettings,
        @Schema(description = "Whether onboarding is completed")
        boolean onboardingCompleted,
        @Schema(description = "Selected interest tags")
        @NonNull List<String> interests,
        @Schema(description = "TON wallet address", example = "UQBx...")
        @Nullable String tonAddress,
        @Schema(description = "Registration timestamp")
        @NonNull Instant createdAt
) {

    /** Defensive copy constructor. */
    public UserProfile {
        interests = List.copyOf(interests);
    }
}
