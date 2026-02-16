package com.advertmarket.marketplace.api.dto.creative;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Media asset attached to a creative template.
 *
 * @param id media asset id
 * @param type media type
 * @param url canonical media URL used in preview/publication
 * @param thumbnailUrl optional thumbnail URL
 * @param fileName original file name
 * @param fileSize human-readable size
 * @param mimeType MIME type
 * @param sizeBytes raw size in bytes
 * @param caption optional caption
 */
public record CreativeMediaAssetDto(
        @NotBlank @Size(max = 64) String id,
        @NotNull CreativeMediaType type,
        @NotBlank @Size(max = 2048) String url,
        @Size(max = 2048) @Nullable String thumbnailUrl,
        @Size(max = 255) @Nullable String fileName,
        @Size(max = 32) @Nullable String fileSize,
        @Size(max = 128) @Nullable String mimeType,
        @PositiveOrZero @Nullable Long sizeBytes,
        @Size(max = 1024) @Nullable String caption
) {
}

