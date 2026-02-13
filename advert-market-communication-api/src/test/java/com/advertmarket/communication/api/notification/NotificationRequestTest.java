package com.advertmarket.communication.api.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationRequest")
class NotificationRequestTest {

    @Test
    @DisplayName("Creates defensive copy of variables map")
    void defensiveCopy_variables() {
        var vars = new HashMap<>(
                Map.of("channel_name", "MyChannel"));

        var request = new NotificationRequest(
                123L, NotificationType.NEW_OFFER, vars);

        vars.put("extra", "value");

        assertThat(request.variables()).hasSize(1);
    }

    @Test
    @DisplayName("Variables map is immutable")
    void variables_isImmutable() {
        var request = new NotificationRequest(
                123L, NotificationType.NEW_OFFER,
                Map.of("k", "v"));

        assertThatThrownBy(
                () -> request.variables().put("x", "y"))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Rejects null type")
    void rejectsNullType() {
        assertThatThrownBy(
                () -> new NotificationRequest(
                        123L, null, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
    }

    @Test
    @DisplayName("Rejects null variables")
    void rejectsNullVariables() {
        assertThatThrownBy(
                () -> new NotificationRequest(
                        123L, NotificationType.NEW_OFFER, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("variables");
    }
}
