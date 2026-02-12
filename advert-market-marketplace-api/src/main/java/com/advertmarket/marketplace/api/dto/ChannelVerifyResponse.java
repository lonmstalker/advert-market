package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Response containing verification results for a Telegram channel.
 *
 * @param channelId       Telegram chat identifier
 * @param title           channel title
 * @param username        public username
 * @param subscriberCount number of subscribers
 * @param botStatus       bot membership and permissions info
 * @param userStatus      requesting user's membership info
 */
@Schema(description = "Channel verification result")
public record ChannelVerifyResponse(
        long channelId,
        @NonNull String title,
        @Nullable String username,
        int subscriberCount,
        @NonNull BotStatus botStatus,
        @NonNull UserStatus userStatus
) {

    /**
     * Bot membership and permissions in the channel.
     *
     * @param isAdmin            whether the bot is an admin
     * @param canPostMessages    can post messages
     * @param canEditMessages    can edit messages
     * @param missingPermissions list of missing required permissions
     */
    @Schema(description = "Bot status in the channel")
    public record BotStatus(
            boolean isAdmin,
            boolean canPostMessages,
            boolean canEditMessages,
            @NonNull List<String> missingPermissions
    ) {
    }

    /**
     * Requesting user's membership status in the channel.
     *
     * @param isMember whether the user is a member
     * @param role     user's role (CREATOR, ADMINISTRATOR, MEMBER, etc.)
     */
    @Schema(description = "User status in the channel")
    public record UserStatus(
            boolean isMember,
            @NonNull String role
    ) {
    }
}
