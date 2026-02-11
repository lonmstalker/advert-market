package com.advertmarket.communication.bot.internal.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("MarkdownV2Util")
class MarkdownV2UtilTest {

    @Test
    @DisplayName("Plain text remains unchanged")
    void plainText_unchanged() {
        assertThat(MarkdownV2Util.escape("Hello World"))
                .isEqualTo("Hello World");
    }

    @DisplayName("Escapes single special character")
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "_   | \\_",
            "*   | \\*",
            "[   | \\[",
            "]   | \\]",
            "(   | \\(",
            ")   | \\)",
            "~   | \\~",
            "`   | \\`",
            ">   | \\>",
            "#   | \\#",
            "+   | \\+",
            "-   | \\-",
            "=   | \\=",
            "{   | \\{",
            "}   | \\}",
            ".   | \\.",
            "!   | \\!",
    })
    void singleSpecialChar_escaped(String input, String expected) {
        assertThat(MarkdownV2Util.escape(input.trim()))
                .isEqualTo(expected.trim());
    }

    @Test
    @DisplayName("Escapes pipe character")
    void pipeChar_escaped() {
        assertThat(MarkdownV2Util.escape("|")).isEqualTo("\\|");
    }

    @Test
    @DisplayName("Escapes only special characters in mixed text")
    void mixedText_onlySpecialCharsEscaped() {
        assertThat(MarkdownV2Util.escape("Price: 100.50 TON"))
                .isEqualTo("Price: 100\\.50 TON");
    }

    @Test
    @DisplayName("Escapes multiple special characters")
    void multipleSpecialChars() {
        assertThat(MarkdownV2Util.escape("Hello! (world)"))
                .isEqualTo("Hello\\! \\(world\\)");
    }

    @Test
    @DisplayName("Handles empty string")
    void emptyString() {
        assertThat(MarkdownV2Util.escape("")).isEmpty();
    }

    @Test
    @DisplayName("Escapes hyphen in channel name")
    void channelName_withHyphen() {
        assertThat(MarkdownV2Util.escape("my-channel"))
                .isEqualTo("my\\-channel");
    }

    @Test
    @DisplayName("Keeps Cyrillic text unchanged")
    void cyrillic_unchanged() {
        assertThat(MarkdownV2Util.escape("Привет"))
                .isEqualTo("Привет");
    }
}
