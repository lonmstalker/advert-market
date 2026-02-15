package com.advertmarket.marketplace.channel.mapper;

import com.advertmarket.db.generated.tables.records.CategoriesRecord;
import com.advertmarket.marketplace.api.dto.CategoryDto;
import com.advertmarket.shared.json.JsonFacade;
import java.util.Map;
import org.jooq.JSONB;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for jOOQ {@link CategoriesRecord} to {@link CategoryDto}.
 */
@Mapper(componentModel = "spring")
public interface CategoryDtoMapper {

    /** Maps record to DTO. */
    @Mapping(target = "localizedName", source = "localizedName")
    CategoryDto toDto(CategoriesRecord record,
                      @Context JsonFacade json);

    /**
     * Parses localized name JSON into a {@code lang -> label} map.
     */
    @SuppressWarnings("unchecked")
    default Map<String, String> jsonToMap(JSONB value,
                                          @Context JsonFacade json) {
        if (value == null || value.data() == null) {
            return Map.of();
        }
        return json.fromJson(value.data(), Map.class);
    }
}
