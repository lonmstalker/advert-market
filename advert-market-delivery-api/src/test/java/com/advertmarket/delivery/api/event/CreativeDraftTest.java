package com.advertmarket.delivery.api.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreativeDraft")
class CreativeDraftTest {

    @Test
    @DisplayName("Creates defensive copy of media list")
    void defensiveCopy_mediaList() {
        var media = new ArrayList<>(
                List.of("https://example.com/img.png"));

        var draft = new CreativeDraft(
                "Ad text", media, List.of());

        media.add("https://example.com/extra.png");

        assertThat(draft.media()).hasSize(1);
    }

    @Test
    @DisplayName("Creates defensive copy of buttons list")
    void defensiveCopy_buttonsList() {
        var buttons = new ArrayList<>(List.of(
                new InlineButton("Visit", "https://ex.com")));

        var draft = new CreativeDraft(
                "Ad text", List.of(), buttons);

        buttons.add(new InlineButton("More", "https://x.com"));

        assertThat(draft.buttons()).hasSize(1);
    }

    @Test
    @DisplayName("Media list is immutable")
    void mediaList_isImmutable() {
        var draft = new CreativeDraft(
                "Ad text",
                List.of("https://example.com/img.png"),
                List.of());

        assertThatThrownBy(
                () -> draft.media().add("extra"))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Buttons list is immutable")
    void buttonsList_isImmutable() {
        var draft = new CreativeDraft(
                "Ad text", List.of(),
                List.of(new InlineButton("Go", "https://x.com")));

        assertThatThrownBy(
                () -> draft.buttons().add(
                        new InlineButton("X", "https://x.com")))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }
}
