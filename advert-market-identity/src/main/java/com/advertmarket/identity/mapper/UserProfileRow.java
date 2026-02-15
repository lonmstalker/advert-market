package com.advertmarket.identity.mapper;

import java.time.OffsetDateTime;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.JSON;

/**
 * JOOQ projection row for the {@code users} table used to build {@code UserProfile}.
 */
public record UserProfileRow(
        long id,
        @Nullable String username,
        @NonNull String firstName,
        @Nullable String lastName,
        @Nullable String languageCode,
        @Nullable String displayCurrency,
        @Nullable JSON notificationSettings,
        @Nullable Boolean onboardingCompleted,
        @Nullable String[] interests,
        @Nullable OffsetDateTime createdAt
) {

    /**
     * Creates projection row with defensive copying for mutable components.
     */
    public UserProfileRow {
        if (interests != null) {
            interests = interests.clone();
        }
    }

    @Override
    public @Nullable String[] interests() {
        return interests != null ? interests.clone() : null;
    }
}
