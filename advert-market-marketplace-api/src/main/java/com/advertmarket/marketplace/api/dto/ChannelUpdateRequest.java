package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Request to update channel details.
 *
 * @param description      new description
 * @param categories       new category slugs (if provided, replaces existing)
 * @param pricePerPostNano new base price in nanoTON
 * @param language         new language code
 * @param isActive         new active status
 */
@Schema(description = "Channel update request")
public record ChannelUpdateRequest(
        @Nullable String description,
        @Nullable List<String> categories,
        @Nullable Long pricePerPostNano,
        @Nullable @Size(max = 10) String language,
        @Nullable Boolean isActive
) {
}
