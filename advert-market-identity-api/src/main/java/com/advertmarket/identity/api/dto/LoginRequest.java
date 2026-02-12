package com.advertmarket.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for Telegram Mini App authentication.
 *
 * @param initData the raw Telegram initData query string
 */
@Schema(description = "Telegram Mini App login request")
public record LoginRequest(
        @Schema(description = "Raw Telegram initData query string",
                example = "query_id=AAH...&auth_date=1700000000&hash=abc")
        @NotBlank @Size(max = 4096) String initData
) {
}
