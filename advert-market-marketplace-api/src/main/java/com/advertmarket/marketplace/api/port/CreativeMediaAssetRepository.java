package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.creative.CreativeMediaAssetDto;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Persistence port for uploaded creative media assets.
 */
public interface CreativeMediaAssetRepository {

    /**
     * Persists a newly uploaded media asset for the owner.
     */
    @NonNull
    CreativeMediaAssetDto create(
            long ownerUserId,
            @NonNull CreativeMediaAssetDto mediaAsset);

    /**
     * Finds a media asset by owner and identifier.
     */
    @NonNull
    Optional<CreativeMediaAssetDto> findByOwnerAndId(
            long ownerUserId,
            @NonNull String mediaAssetId);

    /**
     * Soft-deletes a media asset and returns {@code true} when a row was updated.
     */
    boolean softDelete(
            long ownerUserId,
            @NonNull String mediaAssetId);
}
