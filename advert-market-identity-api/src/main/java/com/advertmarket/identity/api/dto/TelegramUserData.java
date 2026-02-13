package com.advertmarket.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Parsed Telegram user data from initData.
 *
 * @param id           Telegram user ID
 * @param firstName    first name
 * @param lastName     last name (optional)
 * @param username     username without @ (optional)
 * @param languageCode IETF language tag (optional)
 */
@Schema(description = "Parsed Telegram user data from initData")
public record TelegramUserData(
        @Schema(description = "Telegram user ID", example = "42")
        long id,
        @Schema(description = "First name", example = "John")
        @JsonProperty("first_name") @NonNull String firstName,
        @Schema(description = "Last name (optional)", example = "Doe")
        @JsonProperty("last_name") @Nullable String lastName,
        @Schema(description = "Username without @", example = "johndoe")
        @Nullable String username,
        @Schema(description = "IETF language tag", example = "en")
        @JsonProperty("language_code") @Nullable String languageCode
) {

    /** Builds display name from first and last name. */
    public @NonNull String displayName() {
        if (lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }
}
