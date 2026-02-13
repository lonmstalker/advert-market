package com.advertmarket.marketplace.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.marketplace.api.model.PostType;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PricingRuleCreateRequest")
class PricingRuleCreateRequestTest {

    @Test
    @DisplayName("Creates defensive copy of postTypes set")
    void defensiveCopy_postTypes() {
        var types = new HashSet<>(Set.of(PostType.REPOST));

        var request = new PricingRuleCreateRequest(
                "Rule", null, types, 1_000_000L, 0);

        types.add(PostType.NATIVE);

        assertThat(request.postTypes()).hasSize(1);
    }

    @Test
    @DisplayName("PostTypes set is immutable")
    void postTypes_isImmutable() {
        var request = new PricingRuleCreateRequest(
                "Rule", null, Set.of(PostType.REPOST), 1_000_000L, 0);

        assertThatThrownBy(
                () -> request.postTypes().add(PostType.NATIVE))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }
}
