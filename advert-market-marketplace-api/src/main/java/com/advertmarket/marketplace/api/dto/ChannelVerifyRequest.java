package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to verify a Telegram channel before registration.
 *
 * @param channelUsername public channel username (without @)
 */
@Schema(description = "Channel verification request")
public record ChannelVerifyRequest(
        @Schema(description = "Public channel username without @",
                example = "mychannel")
        @NotBlank @Size(max = 255) String channelUsername
) {
}
