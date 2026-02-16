package com.advertmarket.marketplace.api.dto.creative;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Text entity describing inline formatting within creative text.
 *
 * @param type entity type
 * @param offset UTF-16 offset in {@code text}
 * @param length UTF-16 length
 * @param url optional URL for {@link CreativeTextEntityType#TEXT_LINK}
 * @param language optional language for {@link CreativeTextEntityType#PRE}
 */
public record CreativeEntityDto(
        @NotNull CreativeTextEntityType type,
        @Min(0) int offset,
        @Min(1) int length,
        @Size(max = 2048) @Nullable String url,
        @Size(max = 64) @Nullable String language
) {
}

