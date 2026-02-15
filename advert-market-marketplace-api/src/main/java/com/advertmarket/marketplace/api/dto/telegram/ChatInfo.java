package com.advertmarket.marketplace.api.dto.telegram;

import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Information about a Telegram chat (channel/group).
 *
 * <p>Does not include subscriber count — use
 * {@link TelegramChannelPort#getChatMemberCount} separately.
 *
 * @param id          unique Telegram chat identifier
 * @param title       chat title
 * @param username    public username (without @), may be null for private chats
 * @param type        chat type (channel, supergroup, group, private)
 * @param description chat description, may be null
 */
@Schema(description = "Telegram chat metadata used for channel verification.")
public record ChatInfo(
        long id,
        @NonNull String title,
        @Nullable String username,
        @NonNull String type,
        @Nullable String description
) {
}
