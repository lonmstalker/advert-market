package com.advertmarket.identity.api.port;

import com.advertmarket.identity.api.dto.NotificationSettings;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.dto.CurrencyMode;
import com.advertmarket.shared.model.UserId;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Repository for user persistence operations.
 */
public interface UserRepository {

    /**
     * Inserts or updates a user from Telegram data.
     *
     * @param data Telegram user data
     * @return true if the user is an operator
     */
    boolean upsert(@NonNull TelegramUserData data);

    /**
     * Finds a user profile by ID.
     *
     * @param userId user identifier
     * @return user profile, or empty if not found
     */
    @NonNull
    Optional<UserProfile> findById(@NonNull UserId userId);

    /**
     * Marks onboarding as completed and saves interests.
     *
     * @param userId    user identifier
     * @param interests selected interest tags
     */
    void completeOnboarding(@NonNull UserId userId,
            @NonNull List<String> interests);

    /**
     * Updates the user's language code.
     *
     * @param userId       user identifier
     * @param languageCode IETF language tag
     */
    void updateLanguage(@NonNull UserId userId,
            @NonNull String languageCode);

    /**
     * Updates the user's display currency.
     *
     * @param userId   user identifier
     * @param currency ISO 4217 currency code
     */
    void updateDisplayCurrency(@NonNull UserId userId,
            @NonNull String currency);

    /**
     * Updates the user's currency mode.
     *
     * @param userId user identifier
     * @param mode currency selection mode
     */
    void updateCurrencyMode(@NonNull UserId userId,
            @NonNull CurrencyMode mode);

    /**
     * Updates the user's notification settings.
     *
     * @param userId   user identifier
     * @param settings notification preferences
     */
    void updateNotificationSettings(@NonNull UserId userId,
            @NonNull NotificationSettings settings);

    /**
     * Soft-deletes a user by clearing PII and setting the deleted flag.
     *
     * @param userId user identifier
     */
    void softDelete(@NonNull UserId userId);
}
