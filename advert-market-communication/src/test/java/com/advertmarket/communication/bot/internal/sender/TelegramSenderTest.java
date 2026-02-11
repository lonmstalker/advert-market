package com.advertmarket.communication.bot.internal.sender;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.communication.bot.internal.error.BotErrorCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TelegramSender error classification")
class TelegramSenderTest {

    @Test
    @DisplayName("Classifies 429 as rate limited")
    void classifyError_rateLimited() {
        assertThat(TelegramSender.classifyError(429))
                .isEqualTo(BotErrorCategory.RATE_LIMITED);
    }

    @Test
    @DisplayName("Classifies 403 as user blocked")
    void classifyError_userBlocked() {
        assertThat(TelegramSender.classifyError(403))
                .isEqualTo(BotErrorCategory.USER_BLOCKED);
    }

    @Test
    @DisplayName("Classifies 400 as client error")
    void classifyError_badRequest() {
        assertThat(TelegramSender.classifyError(400))
                .isEqualTo(BotErrorCategory.CLIENT_ERROR);
    }

    @Test
    @DisplayName("Classifies 401 as client error")
    void classifyError_unauthorized() {
        assertThat(TelegramSender.classifyError(401))
                .isEqualTo(BotErrorCategory.CLIENT_ERROR);
    }

    @Test
    @DisplayName("Classifies 500 as server error")
    void classifyError_internalServerError() {
        assertThat(TelegramSender.classifyError(500))
                .isEqualTo(BotErrorCategory.SERVER_ERROR);
    }

    @Test
    @DisplayName("Classifies 502 as server error")
    void classifyError_badGateway() {
        assertThat(TelegramSender.classifyError(502))
                .isEqualTo(BotErrorCategory.SERVER_ERROR);
    }

    @Test
    @DisplayName("Classifies unknown code as unknown")
    void classifyError_unknownCode() {
        assertThat(TelegramSender.classifyError(0))
                .isEqualTo(BotErrorCategory.UNKNOWN);
    }
}
