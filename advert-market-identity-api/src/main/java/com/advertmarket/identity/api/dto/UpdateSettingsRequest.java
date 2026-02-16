package com.advertmarket.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Request body for updating user settings (currency and/or notifications).
 *
 * @param displayCurrency      fiat display currency code
 * @param currencyMode         currency selection mode
 * @param notificationSettings notification preferences
 */
@Schema(description = "Update user settings request")
public record UpdateSettingsRequest(
        @Schema(description = "Fiat display currency code", example = "RUB")
        @Nullable @Size(min = 3, max = 3)
        String displayCurrency,
        @Schema(description = "Currency selection mode", example = "AUTO")
        @Nullable
        CurrencyMode currencyMode,
        @Schema(description = "Notification preferences")
        @Nullable @Valid
        NotificationSettings notificationSettings
) {

    /** Bean validation rule: MANUAL mode requires explicit currency. */
    @AssertTrue(message = "displayCurrency is required when currencyMode is MANUAL")
    public boolean isManualModeValid() {
        return currencyMode != CurrencyMode.MANUAL || displayCurrency != null;
    }
}
