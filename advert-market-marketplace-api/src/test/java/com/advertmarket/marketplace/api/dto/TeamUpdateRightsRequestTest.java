package com.advertmarket.marketplace.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.marketplace.api.model.ChannelRight;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TeamUpdateRightsRequest")
class TeamUpdateRightsRequestTest {

    @Test
    @DisplayName("Creates defensive copy of rights set")
    void defensiveCopy_rights() {
        var rights = new HashSet<>(Set.of(ChannelRight.PUBLISH));

        var request = new TeamUpdateRightsRequest(rights);

        rights.add(ChannelRight.MODERATE);

        assertThat(request.rights()).hasSize(1);
    }

    @Test
    @DisplayName("Rights set is immutable")
    void rights_isImmutable() {
        var request = new TeamUpdateRightsRequest(
                Set.of(ChannelRight.PUBLISH));

        assertThatThrownBy(
                () -> request.rights().add(ChannelRight.MODERATE))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }
}
