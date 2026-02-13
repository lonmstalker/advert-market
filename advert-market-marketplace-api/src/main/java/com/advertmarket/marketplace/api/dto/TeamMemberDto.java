package com.advertmarket.marketplace.api.dto;

import com.advertmarket.marketplace.api.model.ChannelMembershipRole;
import com.advertmarket.marketplace.api.model.ChannelRight;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Channel team member representation.
 *
 * @param userId    member user ID
 * @param username  Telegram username (nullable)
 * @param firstName user first name
 * @param role      membership role
 * @param rights    granular rights (relevant for MANAGER)
 * @param invitedBy user who invited this member (null for OWNER)
 * @param createdAt membership creation timestamp
 */
@Schema(description = "Channel team member")
public record TeamMemberDto(
        long userId,
        @Nullable String username,
        @NonNull String firstName,
        @NonNull ChannelMembershipRole role,
        @NonNull Set<ChannelRight> rights,
        @Nullable Long invitedBy,
        @NonNull OffsetDateTime createdAt
) {

    /** Defensively copies rights. */
    public TeamMemberDto {
        rights = Set.copyOf(rights);
    }
}
