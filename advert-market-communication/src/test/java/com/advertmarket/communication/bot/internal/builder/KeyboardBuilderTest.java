package com.advertmarket.communication.bot.internal.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KeyboardBuilderTest {

    @Test
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
    void urlButton() {
        var markup = KeyboardBuilder.inline()
                .urlButton("Link", "https://example.com")
                .build();

        var rows = markup.inlineKeyboard();
        assertThat(rows.length).isEqualTo(1);
        assertThat(rows[0].length).isEqualTo(1);
    }

    @Test
    void webAppButton() {
        var markup = KeyboardBuilder.inline()
                .webAppButton("Open", "https://app.example.com")
                .build();

        var rows = markup.inlineKeyboard();
        assertThat(rows.length).isEqualTo(1);
        assertThat(rows[0].length).isEqualTo(1);
    }

    @Test
    void emptyRowIsSkipped() {
        var markup = KeyboardBuilder.inline()
                .row()
                .callbackButton("A", "a")
                .build();

        var rows = markup.inlineKeyboard();
        assertThat(rows.length).isEqualTo(1);
    }
}
