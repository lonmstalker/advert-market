package com.advertmarket.marketplace.api.dto.creative;

import java.time.OffsetDateTime;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Historical creative draft version.
 *
 * @param version version number
 * @param draft snapshot of draft payload
 * @param createdAt version creation timestamp
 */
public record CreativeVersionDto(
        int version,
        @NonNull CreativeDraftDto draft,
        @NonNull OffsetDateTime createdAt
) {
}

