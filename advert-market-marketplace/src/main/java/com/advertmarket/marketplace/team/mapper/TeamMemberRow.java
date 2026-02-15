package com.advertmarket.marketplace.team.mapper;

import java.time.OffsetDateTime;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.JSONB;

/**
 * JOOQ query projection for channel team members.
 */
public record TeamMemberRow(
        long userId,
        @Nullable String username,
        @NonNull String firstName,
        @NonNull String role,
        @Nullable JSONB rights,
        @Nullable Long invitedBy,
        @NonNull OffsetDateTime createdAt) {
}
