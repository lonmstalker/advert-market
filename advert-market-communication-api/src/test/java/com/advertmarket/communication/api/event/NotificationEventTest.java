package com.advertmarket.communication.api.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationEvent")
class NotificationEventTest {

    @Test
    @DisplayName("Creates defensive copy of vars map")
    void defensiveCopy_varsMap() {
        var vars = new HashMap<>(
                Map.of("amount", "1.5 TON"));

        var event = new NotificationEvent(
                123L, "PAYOUT", "ru", vars, null);

        vars.put("extra", "value");

        assertThat(event.vars()).hasSize(1);
    }

    @Test
    @DisplayName("Vars map is immutable")
    void varsMap_isImmutable() {
        var event = new NotificationEvent(
                123L, "PAYOUT", "ru",
                Map.of("k", "v"), null);

        assertThatThrownBy(
                () -> event.vars().put("x", "y"))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Creates defensive copy of buttons list")
    void defensiveCopy_buttonsList() {
        var buttons = new ArrayList<>(List.of(
                new NotificationButton(
                        "View", "https://app.com/deal/1")));

        var event = new NotificationEvent(
                123L, "OFFER", "en",
                Map.of(), buttons);

        buttons.add(new NotificationButton(
                "X", "https://x.com"));

        assertThat(event.buttons()).hasSize(1);
    }

    @Test
    @DisplayName("Null buttons is allowed")
    void nullButtons_isAllowed() {
        var event = new NotificationEvent(
                123L, "PAYOUT", "ru", Map.of(), null);

        assertThat(event.buttons()).isNull();
    }

    @Test
    @DisplayName("Buttons list is immutable when present")
    void buttonsList_isImmutable() {
        var event = new NotificationEvent(
                123L, "OFFER", "en", Map.of(),
                List.of(new NotificationButton(
                        "View", "https://app.com")));

        assertThatThrownBy(
                () -> event.buttons().add(
                        new NotificationButton(
                                "X", "https://x.com")))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }
}
