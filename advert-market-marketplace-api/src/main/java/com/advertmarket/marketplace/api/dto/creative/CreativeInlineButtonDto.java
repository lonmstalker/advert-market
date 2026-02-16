package com.advertmarket.marketplace.api.dto.creative;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Inline keyboard button in creative drafts.
 *
 * @param id stable client-side id
 * @param text button label
 * @param url optional button URL
 */
public record CreativeInlineButtonDto(
        @NotBlank @Size(max = 64) String id,
        @NotBlank @Size(max = 50) String text,
        @Size(max = 2048) @Nullable String url
) {
}

