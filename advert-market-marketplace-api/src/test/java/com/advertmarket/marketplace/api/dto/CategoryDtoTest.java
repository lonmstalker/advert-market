package com.advertmarket.marketplace.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CategoryDto")
class CategoryDtoTest {

    @Test
    @DisplayName("Creates defensive copy of localizedName map")
    void defensiveCopy_localizedName() {
        var names = new HashMap<>(Map.of("en", "Tech"));

        var dto = new CategoryDto(1, "tech", names, 0);

        names.put("ru", "Технологии");

        assertThat(dto.localizedName()).hasSize(1);
    }

    @Test
    @DisplayName("LocalizedName map is immutable")
    void localizedName_isImmutable() {
        var dto = new CategoryDto(
                1, "tech", Map.of("en", "Tech"), 0);

        assertThatThrownBy(
                () -> dto.localizedName().put("ru", "Технологии"))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }
}
