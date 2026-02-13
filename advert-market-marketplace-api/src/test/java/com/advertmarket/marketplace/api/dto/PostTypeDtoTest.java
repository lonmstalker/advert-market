package com.advertmarket.marketplace.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.marketplace.api.model.PostType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PostTypeDto")
class PostTypeDtoTest {

    @Test
    @DisplayName("Creates defensive copy of localizedName map")
    void defensiveCopy_localizedName() {
        var names = new HashMap<>(Map.of("en", "Repost"));

        var dto = new PostTypeDto(PostType.REPOST, names);

        names.put("ru", "Репост");

        assertThat(dto.localizedName()).hasSize(1);
    }

    @Test
    @DisplayName("LocalizedName map is immutable")
    void localizedName_isImmutable() {
        var dto = new PostTypeDto(
                PostType.REPOST, Map.of("en", "Repost"));

        assertThatThrownBy(
                () -> dto.localizedName().put("ru", "Репост"))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }
}
