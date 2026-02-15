package com.advertmarket.shared.pii;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for PII encryption at rest.
 *
 * @param key Base64-encoded 256-bit AES master key
 */
@ConfigurationProperties(prefix = "app.pii.encryption")
@PropertyGroupDoc(
        displayName = "PII Encryption",
        description = "PII data encryption at rest using AES-256-GCM",
        category = "Security"
)
public record PiiEncryptionProperties(
        @PropertyDoc(
                description = "Base64-encoded 256-bit AES master key for PII encryption",
                required = Requirement.REQUIRED
        )
        @NonNull String key
) {

    /** Validates key is not blank. */
    public PiiEncryptionProperties {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("PII encryption key must not be blank");
        }
    }
}
