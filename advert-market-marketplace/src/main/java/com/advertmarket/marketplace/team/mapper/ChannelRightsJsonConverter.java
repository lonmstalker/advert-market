package com.advertmarket.marketplace.team.mapper;

import com.advertmarket.marketplace.api.model.ChannelRight;
import com.advertmarket.shared.json.JsonFacade;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.JSONB;

/**
 * Converts channel rights between JSON representation and domain enums.
 */
public final class ChannelRightsJsonConverter {

    private ChannelRightsJsonConverter() {
    }

    /**
     * Parses rights JSON into a set of enabled {@link ChannelRight} values.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static Set<ChannelRight> parseRights(
            @Nullable JSONB json,
            @NonNull JsonFacade jsonFacade) {
        if (json == null || json.data() == null
                || json.data().isBlank()
                || "{}".equals(json.data())) {
            return Set.of();
        }

        Map<String, Object> map = jsonFacade.fromJson(
                json.data(), Map.class);
        var result = EnumSet.noneOf(ChannelRight.class);
        for (var entry : map.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                result.add(ChannelRight.valueOf(
                        entry.getKey().toUpperCase()));
            }
        }
        return Set.copyOf(result);
    }

    /**
     * Converts rights set to JSON representation (lowercase keys with {@code true} values).
     */
    @NonNull
    public static JSONB rightsToJson(
            @NonNull Set<ChannelRight> rights,
            @NonNull JsonFacade jsonFacade) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (ChannelRight right : rights) {
            map.put(right.name().toLowerCase(), true);
        }
        return JSONB.jsonb(jsonFacade.toJson(map));
    }
}
