package com.advertmarket.identity.api.port;

import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.shared.model.UserId;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
     * @return user profile, or null if not found
     */
    @Nullable
    UserProfile findById(@NonNull UserId userId);

    /**
     * Marks onboarding as completed and saves interests.
     *
     * @param userId    user identifier
     * @param interests selected interest tags
     */
    void completeOnboarding(@NonNull UserId userId,
            @NonNull List<String> interests);

    /**
     * Soft-deletes a user by clearing PII and setting the deleted flag.
     *
     * @param userId user identifier
     */
    void softDelete(@NonNull UserId userId);
}
