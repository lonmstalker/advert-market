package com.advertmarket.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for Telegram Mini App authentication.
 *
 * @param initData the raw Telegram initData query string
 */
public record LoginRequest(
        @NotBlank String initData
) {
}
