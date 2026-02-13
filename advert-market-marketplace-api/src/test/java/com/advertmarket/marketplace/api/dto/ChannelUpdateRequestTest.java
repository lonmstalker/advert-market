package com.advertmarket.marketplace.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChannelUpdateRequest")
class ChannelUpdateRequestTest {

    @Test
    @DisplayName("Creates defensive copy of categories list")
    void defensiveCopy_categories() {
        var cats = new ArrayList<>(List.of("tech"));

        var request = new ChannelUpdateRequest(
                null, cats, null, null, null);

        cats.add("crypto");

        assertThat(request.categories()).hasSize(1);
    }

    @Test
    @DisplayName("Categories list is immutable when present")
    void categories_isImmutable() {
        var request = new ChannelUpdateRequest(
                null, List.of("tech"), null, null, null);

        assertThatThrownBy(
                () -> request.categories().add("crypto"))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Null categories is allowed")
    void nullCategories_isAllowed() {
        var request = new ChannelUpdateRequest(
                null, null, null, null, null);

        assertThat(request.categories()).isNull();
    }
}
