package com.advertmarket.communication.bot.internal.sender;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.communication.bot.internal.error.BotErrorCategory;
import org.junit.jupiter.api.Test;

class TelegramSenderTest {

    @Test
    void classifyError_rateLimited() {
        assertThat(TelegramSender.classifyError(429))
                .isEqualTo(BotErrorCategory.RATE_LIMITED);
    }

    @Test
    void classifyError_userBlocked() {
        assertThat(TelegramSender.classifyError(403))
                .isEqualTo(BotErrorCategory.USER_BLOCKED);
    }

    @Test
    void classifyError_badRequest() {
        assertThat(TelegramSender.classifyError(400))
                .isEqualTo(BotErrorCategory.CLIENT_ERROR);
    }

    @Test
    void classifyError_unauthorized() {
        assertThat(TelegramSender.classifyError(401))
                .isEqualTo(BotErrorCategory.CLIENT_ERROR);
    }

    @Test
    void classifyError_internalServerError() {
        assertThat(TelegramSender.classifyError(500))
                .isEqualTo(BotErrorCategory.SERVER_ERROR);
    }

    @Test
    void classifyError_badGateway() {
        assertThat(TelegramSender.classifyError(502))
                .isEqualTo(BotErrorCategory.SERVER_ERROR);
    }

    @Test
    void classifyError_unknownCode() {
        assertThat(TelegramSender.classifyError(0))
                .isEqualTo(BotErrorCategory.UNKNOWN);
    }
}
