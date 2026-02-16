package com.advertmarket.marketplace.creative.repository;

import static com.advertmarket.db.generated.tables.CreativeMediaAssets.CREATIVE_MEDIA_ASSETS;

import com.advertmarket.marketplace.api.dto.creative.CreativeMediaAssetDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeMediaType;
import com.advertmarket.marketplace.api.port.CreativeMediaAssetRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter for creative media assets.
 */
@Repository
@RequiredArgsConstructor
public class JooqCreativeMediaAssetRepository
        implements CreativeMediaAssetRepository {

    private final DSLContext dsl;

    @Override
    @NonNull
    public CreativeMediaAssetDto create(
            long ownerUserId,
            @NonNull CreativeMediaAssetDto mediaAsset) {
        UUID mediaId = UUID.fromString(mediaAsset.id());
        var record = dsl.insertInto(CREATIVE_MEDIA_ASSETS)
                .set(CREATIVE_MEDIA_ASSETS.ID, mediaId)
                .set(CREATIVE_MEDIA_ASSETS.OWNER_USER_ID, ownerUserId)
                .set(CREATIVE_MEDIA_ASSETS.MEDIA_TYPE, mediaAsset.type().name())
                .set(CREATIVE_MEDIA_ASSETS.URL, mediaAsset.url())
                .set(CREATIVE_MEDIA_ASSETS.THUMBNAIL_URL, mediaAsset.thumbnailUrl())
                .set(CREATIVE_MEDIA_ASSETS.FILE_NAME, mediaAsset.fileName())
                .set(CREATIVE_MEDIA_ASSETS.FILE_SIZE, mediaAsset.fileSize())
                .set(CREATIVE_MEDIA_ASSETS.MIME_TYPE, mediaAsset.mimeType())
                .set(CREATIVE_MEDIA_ASSETS.SIZE_BYTES, mediaAsset.sizeBytes())
                .set(CREATIVE_MEDIA_ASSETS.CAPTION, mediaAsset.caption())
                .set(CREATIVE_MEDIA_ASSETS.IS_DELETED, false)
                .returning()
                .fetchSingle();
        return toDto(record);
    }

    @Override
    @NonNull
    public Optional<CreativeMediaAssetDto> findByOwnerAndId(
            long ownerUserId,
            @NonNull String mediaAssetId) {
        Optional<UUID> uuid = parseUuid(mediaAssetId);
        if (uuid.isEmpty()) {
            return Optional.empty();
        }
        return dsl.selectFrom(CREATIVE_MEDIA_ASSETS)
                .where(CREATIVE_MEDIA_ASSETS.ID.eq(uuid.get()))
                .and(CREATIVE_MEDIA_ASSETS.OWNER_USER_ID.eq(ownerUserId))
                .and(CREATIVE_MEDIA_ASSETS.IS_DELETED.isFalse())
                .fetchOptional()
                .map(this::toDto);
    }

    @Override
    public boolean softDelete(
            long ownerUserId,
            @NonNull String mediaAssetId) {
        Optional<UUID> uuid = parseUuid(mediaAssetId);
        if (uuid.isEmpty()) {
            return false;
        }
        int rows = dsl.update(CREATIVE_MEDIA_ASSETS)
                .set(CREATIVE_MEDIA_ASSETS.IS_DELETED, true)
                .set(CREATIVE_MEDIA_ASSETS.DELETED_AT, OffsetDateTime.now())
                .where(CREATIVE_MEDIA_ASSETS.ID.eq(uuid.get()))
                .and(CREATIVE_MEDIA_ASSETS.OWNER_USER_ID.eq(ownerUserId))
                .and(CREATIVE_MEDIA_ASSETS.IS_DELETED.isFalse())
                .execute();
        return rows > 0;
    }

    private CreativeMediaAssetDto toDto(
            com.advertmarket.db.generated.tables.records.CreativeMediaAssetsRecord record) {
        return new CreativeMediaAssetDto(
                record.getId().toString(),
                CreativeMediaType.valueOf(record.getMediaType()),
                record.getUrl(),
                record.getThumbnailUrl(),
                record.getFileName(),
                record.getFileSize(),
                record.getMimeType(),
                record.getSizeBytes(),
                record.getCaption());
    }

    private Optional<UUID> parseUuid(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
