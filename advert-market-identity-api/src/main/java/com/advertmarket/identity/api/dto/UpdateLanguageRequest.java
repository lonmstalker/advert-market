package com.advertmarket.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Request body for updating user language.
 *
 * @param languageCode IETF language tag
 */
@Schema(description = "Update user language request")
public record UpdateLanguageRequest(
        @Schema(description = "IETF language tag", example = "ru")
        @NotBlank @Size(max = 5)
        @NonNull String languageCode
) {}
