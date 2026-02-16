package com.advertmarket.marketplace.creative.storage;

import com.advertmarket.marketplace.api.dto.creative.CreativeMediaType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Uploads creative media to external object storage.
 */
public interface CreativeMediaStorage {

    /**
     * Stores uploaded media and returns normalized metadata.
     */
    @NonNull
    StoredCreativeMedia store(
            long ownerUserId,
            @NonNull String mediaAssetId,
            @NonNull CreativeMediaType mediaType,
            @NonNull MultipartFile file);
}
