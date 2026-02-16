package com.advertmarket.marketplace.creative.storage;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Stored media metadata returned by object storage adapter.
 */
public record StoredCreativeMedia(
        @NonNull String url,
        @Nullable String thumbnailUrl,
        @Nullable String mimeType,
        @Nullable Long sizeBytes,
        @Nullable String fileName
) {
}
