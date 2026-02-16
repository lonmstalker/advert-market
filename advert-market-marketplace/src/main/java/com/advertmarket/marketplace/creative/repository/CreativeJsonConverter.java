package com.advertmarket.marketplace.creative.repository;

import com.advertmarket.marketplace.api.dto.creative.CreativeDraftDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeMediaAssetDto;
import com.advertmarket.shared.json.JsonFacade;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.JSONB;

/**
 * Converts creative JSONB columns to domain DTOs and back.
 */
final class CreativeJsonConverter {

    private CreativeJsonConverter() {
    }

    @NonNull
    static JSONB draftToJson(
            @NonNull CreativeDraftDto draft,
            @NonNull JsonFacade jsonFacade) {
        return JSONB.jsonb(jsonFacade.toJson(draft));
    }

    @NonNull
    static CreativeDraftDto draftFromJson(
            JSONB draftJson,
            @NonNull JsonFacade jsonFacade) {
        if (draftJson == null || draftJson.data() == null
                || draftJson.data().isBlank()) {
            return new CreativeDraftDto("", java.util.List.of(),
                    java.util.List.of(), java.util.List.of(), false);
        }
        return jsonFacade.fromJson(draftJson.data(), CreativeDraftDto.class);
    }

    @NonNull
    static JSONB mediaToJson(
            @NonNull CreativeMediaAssetDto media,
            @NonNull JsonFacade jsonFacade) {
        return JSONB.jsonb(jsonFacade.toJson(media));
    }

    @NonNull
    static CreativeMediaAssetDto mediaFromJson(
            JSONB mediaJson,
            @NonNull JsonFacade jsonFacade) {
        if (mediaJson == null || mediaJson.data() == null
                || mediaJson.data().isBlank()) {
            throw new IllegalArgumentException("Media payload is empty");
        }
        return jsonFacade.fromJson(mediaJson.data(), CreativeMediaAssetDto.class);
    }

    @NonNull
    static String readCursorField(
            @NonNull Map<String, String> fields,
            @NonNull String key) {
        String value = fields.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Cursor field is missing: " + key);
        }
        return value;
    }
}

