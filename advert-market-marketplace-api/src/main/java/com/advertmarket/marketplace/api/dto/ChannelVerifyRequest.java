package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Request to verify a Telegram channel before registration.
 *
 * @param channelUsername channel reference:
 *                        {@code @username}, t.me link, private post link, or id
 */
@Schema(description = "Channel verification request")
public record ChannelVerifyRequest(
        @Schema(description = "Channel reference: username, t.me link, "
                + "private post link, or numeric id",
                example = "mychannel")
        @NotBlank @Size(max = 255) @NonNull String channelUsername
) {
}
