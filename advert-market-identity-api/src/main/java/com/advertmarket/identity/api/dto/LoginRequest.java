package com.advertmarket.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for Telegram Mini App authentication.
 *
 * @param initData the raw Telegram initData query string
 */
public record LoginRequest(
        @NotBlank @Size(max = 4096) String initData
) {
}
