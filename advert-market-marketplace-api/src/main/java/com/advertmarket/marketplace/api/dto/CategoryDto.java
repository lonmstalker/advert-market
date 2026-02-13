package com.advertmarket.marketplace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Category representation with localized names.
 *
 * @param id            category ID
 * @param slug          unique category slug
 * @param localizedName localized display names (lang code → name)
 * @param sortOrder     display order
 */
@Schema(description = "Channel category with localized names")
public record CategoryDto(
        int id,
        @NonNull String slug,
        @NonNull Map<String, String> localizedName,
        int sortOrder
) {
}
