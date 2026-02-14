package com.advertmarket.communication.bot.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StartCommand")
class StartCommandTest {

    @Test
    @DisplayName("Prefixes welcome message with custom emoji when configured")
    void maybePrefixCustomEmoji_withId() {
        assertThat(StartCommand.maybePrefixCustomEmoji(
                "Hello", "123"))
                .isEqualTo("![⭐](tg://emoji?id=123) Hello");
    }

    @Test
    @DisplayName("Does not prefix welcome message when emoji id is blank")
    void maybePrefixCustomEmoji_blank() {
        assertThat(StartCommand.maybePrefixCustomEmoji(
                "Hello", ""))
                .isEqualTo("Hello");
    }

    @Test
    @DisplayName("Does not prefix welcome message when emoji id is invalid")
    void maybePrefixCustomEmoji_invalid() {
        assertThat(StartCommand.maybePrefixCustomEmoji(
                "Hello", "abc"))
                .isEqualTo("Hello");
    }

    @Test
    @DisplayName("Extracts deep link parameter")
    void extractDeepLinkParam_withParam() {
        assertThat(StartCommand.extractDeepLinkParam(
                "/start deal_123")).isEqualTo("deal_123");
    }

    @Test
    @DisplayName("Returns null when no parameter")
    void extractDeepLinkParam_noParam() {
        assertThat(StartCommand.extractDeepLinkParam("/start"))
                .isNull();
    }

    @Test
    @DisplayName("Returns null for null text")
    void extractDeepLinkParam_nullText() {
        assertThat(StartCommand.extractDeepLinkParam(null))
                .isNull();
    }

    @Test
    @DisplayName("Resolves deal deep link route")
    void resolveRoute_deal() {
        assertThat(StartCommand.resolveRoute("deal_abc"))
                .isEqualTo("/deals/abc");
    }

    @Test
    @DisplayName("Resolves channel deep link route")
    void resolveRoute_channel() {
        assertThat(StartCommand.resolveRoute("channel_xyz"))
                .isEqualTo("/channels/xyz");
    }

    @Test
    @DisplayName("Resolves dispute deep link route")
    void resolveRoute_dispute() {
        assertThat(StartCommand.resolveRoute("dispute_99"))
                .isEqualTo("/disputes/99");
    }

    @Test
    @DisplayName("Resolves deposit deep link route")
    void resolveRoute_deposit() {
        assertThat(StartCommand.resolveRoute("deposit_42"))
                .isEqualTo("/deposit/42");
    }

    @Test
    @DisplayName("Falls back to ref parameter for unknown prefix")
    void resolveRoute_unknownPrefix() {
        assertThat(StartCommand.resolveRoute("unknown_param"))
                .isEqualTo("/?ref=unknown_param");
    }
}
