package com.advertmarket.marketplace.api.dto.creative;

import java.time.OffsetDateTime;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Creative template persisted in the personal creative library.
 *
 * @param id template id
 * @param title template title
 * @param draft current draft payload
 * @param version current template version
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 */
public record CreativeTemplateDto(
        @NonNull String id,
        @NonNull String title,
        @NonNull CreativeDraftDto draft,
        int version,
        @NonNull OffsetDateTime createdAt,
        @NonNull OffsetDateTime updatedAt
) {
}

