package com.advertmarket.marketplace.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChannelVerifyResponse.BotStatus")
class ChannelVerifyResponseTest {

    @Test
    @DisplayName("Creates defensive copy of missingPermissions list")
    void defensiveCopy_missingPermissions() {
        var perms = new ArrayList<>(List.of("can_post_messages"));

        var status = new ChannelVerifyResponse.BotStatus(
                true, false, false, perms);

        perms.add("can_edit_messages");

        assertThat(status.missingPermissions()).hasSize(1);
    }

    @Test
    @DisplayName("MissingPermissions list is immutable")
    void missingPermissions_isImmutable() {
        var status = new ChannelVerifyResponse.BotStatus(
                true, true, true, List.of("can_post_messages"));

        assertThatThrownBy(
                () -> status.missingPermissions().add("extra"))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }
}
