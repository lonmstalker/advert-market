package com.advertmarket.communication.bot.internal.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KeyboardBuilder")
class KeyboardBuilderTest {

    @Test
    @DisplayName("Creates single row with callback buttons")
    void singleRowWithCallbackButtons() {
        var markup = KeyboardBuilder.inline()
                .callbackButton("A", "data_a")
                .callbackButton("B", "data_b")
                .build();

        var rows = markup.inlineKeyboard();
        assertThat(rows.length).isEqualTo(1);
        assertThat(rows[0].length).isEqualTo(2);
    }

    @Test
    @DisplayName("Creates multiple rows")
    void multipleRows() {
        var markup = KeyboardBuilder.inline()
                .callbackButton("A", "a")
                .row()
                .callbackButton("B", "b")
                .build();

        var rows = markup.inlineKeyboard();
        assertThat(rows.length).isEqualTo(2);
        assertThat(rows[0].length).isEqualTo(1);
        assertThat(rows[1].length).isEqualTo(1);
    }

    @Test
    @DisplayName("Creates URL button")
    void urlButton() {
        var markup = KeyboardBuilder.inline()
                .urlButton("Link", "https://example.com")
                .build();

        var rows = markup.inlineKeyboard();
        assertThat(rows.length).isEqualTo(1);
        assertThat(rows[0].length).isEqualTo(1);
    }

    @Test
    @DisplayName("Creates web app button")
    void webAppButton() {
        var markup = KeyboardBuilder.inline()
                .webAppButton("Open", "https://app.example.com")
                .build();

        var rows = markup.inlineKeyboard();
        assertThat(rows.length).isEqualTo(1);
        assertThat(rows[0].length).isEqualTo(1);
    }

    @Test
    @DisplayName("Skips empty rows")
    void emptyRowIsSkipped() {
        var markup = KeyboardBuilder.inline()
                .row()
                .callbackButton("A", "a")
                .build();

        var rows = markup.inlineKeyboard();
        assertThat(rows.length).isEqualTo(1);
    }
}
