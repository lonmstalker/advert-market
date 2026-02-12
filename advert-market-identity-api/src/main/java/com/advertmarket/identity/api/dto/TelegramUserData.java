package com.advertmarket.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public record TelegramUserData(
        long id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") @Nullable String lastName,
        @Nullable String username,
        @JsonProperty("language_code") @Nullable String languageCode
) {
}
