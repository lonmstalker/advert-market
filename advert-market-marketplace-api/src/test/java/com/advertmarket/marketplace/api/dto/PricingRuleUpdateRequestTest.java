package com.advertmarket.marketplace.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.marketplace.api.model.PostType;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PricingRuleUpdateRequest")
class PricingRuleUpdateRequestTest {

    @Test
    @DisplayName("Creates defensive copy of postTypes set")
    void defensiveCopy_postTypes() {
        var types = new HashSet<>(Set.of(PostType.REPOST));

        var request = new PricingRuleUpdateRequest(
                null, null, types, null, null, null);

        types.add(PostType.NATIVE);

        assertThat(request.postTypes()).hasSize(1);
    }

    @Test
    @DisplayName("PostTypes set is immutable when present")
    void postTypes_isImmutable() {
        var request = new PricingRuleUpdateRequest(
                null, null, Set.of(PostType.REPOST), null, null, null);

        assertThatThrownBy(
                () -> request.postTypes().add(PostType.NATIVE))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Null postTypes is allowed")
    void nullPostTypes_isAllowed() {
        var request = new PricingRuleUpdateRequest(
                null, null, null, null, null, null);

        assertThat(request.postTypes()).isNull();
    }
}
