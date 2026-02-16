package com.advertmarket.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Currency selection mode for user profile.
 */
@Schema(description = "Currency selection mode")
public enum CurrencyMode {
    /** Currency is derived from interface language mapping. */
    AUTO,
    /** Currency is fixed by explicit user selection. */
    MANUAL
}
