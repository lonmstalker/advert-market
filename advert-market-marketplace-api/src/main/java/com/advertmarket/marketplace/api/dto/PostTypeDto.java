package com.advertmarket.marketplace.api.dto;

import com.advertmarket.marketplace.api.model.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Post type representation with localized labels.
 *
 * @param value         enum constant
 * @param localizedName localized display names (lang code → label)
 */
@Schema(description = "Post type with localized labels")
public record PostTypeDto(
        @NonNull PostType value,
        @NonNull Map<String, String> localizedName
) {
}
