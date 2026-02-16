package com.advertmarket.marketplace.creative.service;

import com.advertmarket.marketplace.api.dto.creative.CreativeMediaAssetDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeMediaType;
import com.advertmarket.marketplace.api.dto.creative.CreativeTemplateDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeUpsertRequest;
import com.advertmarket.marketplace.api.dto.creative.CreativeVersionDto;
import com.advertmarket.marketplace.api.port.CreativeMediaAssetRepository;
import com.advertmarket.marketplace.api.port.CreativeRepository;
import com.advertmarket.marketplace.creative.storage.CreativeMediaStorage;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.pagination.CursorPage;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Creative template CRUD, versioning, and media upload orchestration.
 */
@Service
@RequiredArgsConstructor
public class CreativeService {

    private static final long BYTES_IN_KB = 1024L;
    private static final long MB = BYTES_IN_KB * BYTES_IN_KB;
    private static final String DOCX_MIME =
            "application/vnd.openxmlformats-officedocument."
                    + "wordprocessingml.document";

    private static final Map<CreativeMediaType, Set<String>> ALLOWED_MIME_TYPES =
            Map.of(
                    CreativeMediaType.PHOTO, Set.of(
                            "image/jpeg",
                            "image/jpg",
                            "image/png",
                            "image/webp"),
                    CreativeMediaType.GIF, Set.of("image/gif"),
                    CreativeMediaType.VIDEO, Set.of(
                            "video/mp4",
                            "video/quicktime",
                            "video/webm"),
                    CreativeMediaType.DOCUMENT, Set.of(
                            "application/pdf",
                            "application/zip",
                            "application/msword",
                            DOCX_MIME,
                            "text/plain"));

    private static final Map<CreativeMediaType, Long> MAX_SIZE_BYTES =
            Map.of(
                    CreativeMediaType.PHOTO, 10L * MB,
                    CreativeMediaType.GIF, 20L * MB,
                    CreativeMediaType.VIDEO, 50L * MB,
                    CreativeMediaType.DOCUMENT, 20L * MB);

    private final CreativeRepository creativeRepository;
    private final CreativeMediaAssetRepository mediaAssetRepository;
    private final CreativeMediaStorage mediaStorage;

    /**
     * Returns paged creative templates for one owner.
     */
    @NonNull
    public CursorPage<CreativeTemplateDto> list(
            long ownerUserId,
            @Nullable String cursor,
            int limit) {
        return creativeRepository.findByOwner(ownerUserId, cursor, limit);
    }

    /**
     * Returns one creative template by id.
     */
    @NonNull
    public CreativeTemplateDto get(long ownerUserId, @NonNull String templateId) {
        return creativeRepository.findByOwnerAndId(ownerUserId, templateId)
                .orElseThrow(() -> creativeNotFound(templateId));
    }

    /**
     * Creates a template and initializes version history.
     */
    @NonNull
    @Transactional
    public CreativeTemplateDto create(
            long ownerUserId,
            @NonNull CreativeUpsertRequest request) {
        return creativeRepository.create(ownerUserId, request);
    }

    /**
     * Updates a template and appends version snapshot.
     */
    @NonNull
    @Transactional
    public CreativeTemplateDto update(
            long ownerUserId,
            @NonNull String templateId,
            @NonNull CreativeUpsertRequest request) {
        return creativeRepository.update(ownerUserId, templateId, request)
                .orElseThrow(() -> creativeNotFound(templateId));
    }

    /**
     * Soft-deletes a creative template.
     */
    @Transactional
    public void delete(long ownerUserId, @NonNull String templateId) {
        if (!creativeRepository.softDelete(ownerUserId, templateId)) {
            throw creativeNotFound(templateId);
        }
    }

    /**
     * Returns all stored template versions.
     */
    @NonNull
    public List<CreativeVersionDto> versions(
            long ownerUserId,
            @NonNull String templateId) {
        if (creativeRepository.findByOwnerAndId(ownerUserId, templateId).isEmpty()) {
            throw creativeNotFound(templateId);
        }
        return creativeRepository.findVersions(ownerUserId, templateId);
    }

    /**
     * Validates and uploads media to object storage.
     */
    @NonNull
    @Transactional
    public CreativeMediaAssetDto uploadMedia(
            long ownerUserId,
            @NonNull MultipartFile file,
            @NonNull CreativeMediaType mediaType,
            @Nullable String caption) {
        validateMedia(file, mediaType);

        String mediaId = UUID.randomUUID().toString();
        var stored = mediaStorage.store(ownerUserId, mediaId, mediaType, file);
        Long storedSizeBytes = stored.sizeBytes();
        String originalName = firstNonBlank(stored.fileName(), file.getOriginalFilename());
        long size = storedSizeBytes != null ? storedSizeBytes : file.getSize();
        String mimeType = firstNonBlank(stored.mimeType(), file.getContentType());

        var dto = new CreativeMediaAssetDto(
                mediaId,
                mediaType,
                stored.url(),
                stored.thumbnailUrl(),
                originalName,
                humanSize(size),
                mimeType,
                size,
                caption == null || caption.isBlank() ? null : caption.trim());
        return mediaAssetRepository.create(ownerUserId, dto);
    }

    /**
     * Soft-deletes media asset metadata.
     */
    @Transactional
    public void deleteMedia(long ownerUserId, @NonNull String mediaAssetId) {
        if (!mediaAssetRepository.softDelete(ownerUserId, mediaAssetId)) {
            throw creativeNotFound(mediaAssetId);
        }
    }

    private void validateMedia(
            MultipartFile file,
            CreativeMediaType mediaType) {
        if (file.isEmpty()) {
            throw new DomainException(
                    ErrorCodes.CREATIVE_INVALID_FORMAT,
                    "Uploaded file is empty");
        }
        String mimeType = file.getContentType();
        Set<String> allowed = ALLOWED_MIME_TYPES.get(mediaType);
        if (mimeType == null || mimeType.isBlank()
                || allowed == null || !allowed.contains(mimeType.toLowerCase())) {
            throw new DomainException(
                    ErrorCodes.CREATIVE_INVALID_FORMAT,
                    "Unsupported media format: " + mimeType);
        }
        long size = file.getSize();
        long maxSize = MAX_SIZE_BYTES.get(mediaType);
        if (size > maxSize) {
            throw new DomainException(
                    ErrorCodes.CREATIVE_TOO_LARGE,
                    "Media exceeds limit: " + size + " > " + maxSize);
        }
    }

    private static String humanSize(long sizeBytes) {
        if (sizeBytes < BYTES_IN_KB) {
            return sizeBytes + " B";
        }
        long kb = sizeBytes / BYTES_IN_KB;
        if (kb < BYTES_IN_KB) {
            return kb + " KB";
        }
        long mb = kb / BYTES_IN_KB;
        return mb + " MB";
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return "file";
    }

    private static DomainException creativeNotFound(String entityId) {
        return new DomainException(
                ErrorCodes.CREATIVE_NOT_FOUND,
                "Creative not found: " + entityId);
    }
}
