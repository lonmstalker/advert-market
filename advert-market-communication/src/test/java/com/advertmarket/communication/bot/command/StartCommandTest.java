package com.advertmarket.communication.bot.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StartCommandTest {

    @Test
    void extractDeepLinkParam_withParam() {
        assertThat(StartCommand.extractDeepLinkParam(
                "/start deal_123")).isEqualTo("deal_123");
    }

    @Test
    void extractDeepLinkParam_noParam() {
        assertThat(StartCommand.extractDeepLinkParam("/start"))
                .isNull();
    }

    @Test
    void extractDeepLinkParam_nullText() {
        assertThat(StartCommand.extractDeepLinkParam(null))
                .isNull();
    }

    @Test
    void resolveRoute_deal() {
        assertThat(StartCommand.resolveRoute("deal_abc"))
                .isEqualTo("/deals/abc");
    }

    @Test
    void resolveRoute_channel() {
        assertThat(StartCommand.resolveRoute("channel_xyz"))
                .isEqualTo("/channels/xyz");
    }

    @Test
    void resolveRoute_dispute() {
        assertThat(StartCommand.resolveRoute("dispute_99"))
                .isEqualTo("/disputes/99");
    }

    @Test
    void resolveRoute_deposit() {
        assertThat(StartCommand.resolveRoute("deposit_42"))
                .isEqualTo("/deposit/42");
    }

    @Test
    void resolveRoute_unknownPrefix() {
        assertThat(StartCommand.resolveRoute("unknown_param"))
                .isEqualTo("/?ref=unknown_param");
    }
}
