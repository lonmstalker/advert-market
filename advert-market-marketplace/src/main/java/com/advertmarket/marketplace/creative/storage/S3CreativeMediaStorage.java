package com.advertmarket.marketplace.creative.storage;

import com.advertmarket.marketplace.api.dto.creative.CreativeMediaType;
import com.advertmarket.marketplace.creative.config.CreativeStorageProperties;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * S3-compatible object storage adapter for creative media assets.
 */
@RequiredArgsConstructor
public class S3CreativeMediaStorage implements CreativeMediaStorage {

    private final S3Client s3Client;
    private final CreativeStorageProperties properties;

    @Override
    @NonNull
    public StoredCreativeMedia store(
            long ownerUserId,
            @NonNull String mediaAssetId,
            @NonNull CreativeMediaType mediaType,
            @NonNull MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String extension = fileExtension(originalName);
        String objectName = extension.isBlank()
                ? mediaAssetId
                : mediaAssetId + "." + extension;
        String key = normalizePath(properties.keyPrefix())
                + "/" + mediaType.name().toLowerCase()
                + "/u-" + ownerUserId + "/" + objectName;

        var request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(file.getContentType())
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return new StoredCreativeMedia(
                    resolvePublicUrl(key),
                    null,
                    file.getContentType(),
                    file.getSize(),
                    originalName);
        } catch (S3Exception | SdkClientException | IOException ex) {
            throw new DomainException(
                    ErrorCodes.SERVICE_UNAVAILABLE,
                    "Failed to upload creative media",
                    ex);
        }
    }

    private String resolvePublicUrl(String key) {
        if (properties.publicBaseUrl() != null
                && !properties.publicBaseUrl().isBlank()) {
            return trimTrailingSlash(properties.publicBaseUrl()) + "/" + key;
        }
        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            return trimTrailingSlash(properties.endpoint())
                    + "/" + properties.bucket() + "/" + key;
        }
        return "s3://" + properties.bucket() + "/" + key;
    }

    private static String fileExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private static String normalizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return "creatives";
        }
        String normalized = raw;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? "creatives" : normalized;
    }

    private static String trimTrailingSlash(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String result = raw;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
