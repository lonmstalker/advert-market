package com.advertmarket.marketplace.creative.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Object storage settings for creative media uploads.
 *
 * @param enabled enables S3-compatible upload adapter
 * @param endpoint optional endpoint override (MinIO/dev)
 * @param region S3 region
 * @param bucket object storage bucket name
 * @param accessKey access key for static credentials
 * @param secretKey secret key for static credentials
 * @param publicBaseUrl public CDN/base URL used in returned media URLs
 * @param keyPrefix object key prefix
 */
@ConfigurationProperties(prefix = "app.marketplace.creatives.storage")
@PropertyGroupDoc(
        displayName = "Creative Storage",
        description = "S3-compatible storage for creative media assets",
        category = "Marketplace"
)
@Validated
public record CreativeStorageProperties(
        @PropertyDoc(
                description = "Enable S3-compatible storage adapter",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("false") boolean enabled,

        @PropertyDoc(
                description = "Custom S3 endpoint URL (use MinIO in development)",
                required = Requirement.OPTIONAL
        )
        String endpoint,

        @PropertyDoc(
                description = "S3 region used by the client",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("us-east-1") String region,

        @PropertyDoc(
                description = "Bucket name for uploaded creative media",
                required = Requirement.OPTIONAL
        )
        @NotBlank String bucket,

        @PropertyDoc(
                description = "S3 access key (dev/prod credentials)",
                required = Requirement.OPTIONAL
        )
        String accessKey,

        @PropertyDoc(
                description = "S3 secret key (dev/prod credentials)",
                required = Requirement.OPTIONAL
        )
        String secretKey,

        @PropertyDoc(
                description = "Public base URL returned to clients",
                required = Requirement.OPTIONAL
        )
        String publicBaseUrl,

        @PropertyDoc(
                description = "Object key prefix under bucket",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("creatives") String keyPrefix
) {
}
