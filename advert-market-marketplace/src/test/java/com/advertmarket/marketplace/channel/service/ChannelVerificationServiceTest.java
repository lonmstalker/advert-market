package com.advertmarket.marketplace.channel.service;

import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_BOT_INSUFFICIENT_RIGHTS;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_BOT_NOT_ADMIN;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_NOT_FOUND;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_USER_NOT_ADMIN;
import static com.advertmarket.shared.exception.ErrorCodes.SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberStatus;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.marketplace.channel.config.ChannelBotProperties;
import com.advertmarket.shared.exception.DomainException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ChannelVerificationService — channel verification logic")
@ExtendWith(MockitoExtension.class)
class ChannelVerificationServiceTest {

    private static final long BOT_ID = 111L;
    private static final long USER_ID = 222L;
    private static final long CHANNEL_ID = -1001234567890L;
    private static final String USERNAME = "testchannel";

    @Mock
    private TelegramChannelPort telegramChannel;

    private ChannelVerificationService service;

    @BeforeEach
    void setUp() {
        var props = new ChannelBotProperties(BOT_ID, Duration.ofSeconds(3));
        service = new ChannelVerificationService(
                telegramChannel, props, Runnable::run);
    }

    @Test
    @DisplayName("Should throw CHANNEL_NOT_FOUND when chat type is not channel")
    void shouldRejectNonChannelType() {
        when(telegramChannel.getChatByUsername(USERNAME))
                .thenReturn(chatInfo("supergroup"));

        assertThatThrownBy(() -> service.verify(USERNAME, USER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(CHANNEL_NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw CHANNEL_BOT_NOT_ADMIN when bot is absent in admin list")
    void shouldRejectBotNotAdminWhenBotMissingInAdmins() {
        when(telegramChannel.getChatByUsername(USERNAME))
                .thenReturn(chatInfo("channel"));
        when(telegramChannel.getChatAdministrators(CHANNEL_ID))
                .thenReturn(List.of(adminInfo(USER_ID)));
        when(telegramChannel.getChatMemberCount(CHANNEL_ID))
                .thenReturn(1000);

        assertThatThrownBy(() -> service.verify(USERNAME, USER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(CHANNEL_BOT_NOT_ADMIN);
    }

    @Test
    @DisplayName("Should throw CHANNEL_BOT_NOT_ADMIN when bot is regular member")
    void shouldRejectBotNotAdmin() {
        when(telegramChannel.getChatByUsername(USERNAME))
                .thenReturn(chatInfo("channel"));
        when(telegramChannel.getChatAdministrators(CHANNEL_ID))
                .thenReturn(List.of(
                        memberInfo(BOT_ID, ChatMemberStatus.MEMBER),
                        adminInfo(USER_ID)));
        when(telegramChannel.getChatMemberCount(CHANNEL_ID))
                .thenReturn(1000);

        assertThatThrownBy(() -> service.verify(USERNAME, USER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(CHANNEL_BOT_NOT_ADMIN);
    }

    @Test
    @DisplayName("Should throw CHANNEL_BOT_INSUFFICIENT_RIGHTS when bot cannot post")
    void shouldRejectBotInsufficientRights() {
        when(telegramChannel.getChatByUsername(USERNAME))
                .thenReturn(chatInfo("channel"));
        when(telegramChannel.getChatAdministrators(CHANNEL_ID))
                .thenReturn(List.of(
                        new ChatMemberInfo(BOT_ID,
                        ChatMemberStatus.ADMINISTRATOR,
                        false, false, false, true),
                        adminInfo(USER_ID)));
        when(telegramChannel.getChatMemberCount(CHANNEL_ID))
                .thenReturn(1000);

        assertThatThrownBy(() -> service.verify(USERNAME, USER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(CHANNEL_BOT_INSUFFICIENT_RIGHTS);
    }

    @Test
    @DisplayName("Should throw CHANNEL_USER_NOT_ADMIN when user is regular member")
    void shouldRejectUserNotAdmin() {
        when(telegramChannel.getChatByUsername(USERNAME))
                .thenReturn(chatInfo("channel"));
        when(telegramChannel.getChatAdministrators(CHANNEL_ID))
                .thenReturn(List.of(botAdmin()));
        when(telegramChannel.getChatMemberCount(CHANNEL_ID))
                .thenReturn(1000);

        assertThatThrownBy(() -> service.verify(USERNAME, USER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(CHANNEL_USER_NOT_ADMIN);
    }

    @Test
    @DisplayName("Should return verification response on happy path")
    void shouldVerifySuccessfully() {
        when(telegramChannel.getChatByUsername(USERNAME))
                .thenReturn(chatInfo("channel"));
        when(telegramChannel.getChatAdministrators(CHANNEL_ID))
                .thenReturn(List.of(botAdmin(), adminInfo(USER_ID)));
        when(telegramChannel.getChatMemberCount(CHANNEL_ID))
                .thenReturn(5000);

        var result = service.verify(USERNAME, USER_ID);

        assertThat(result.channelId()).isEqualTo(CHANNEL_ID);
        assertThat(result.title()).isEqualTo("Test Channel");
        assertThat(result.subscriberCount()).isEqualTo(5000);
        assertThat(result.botStatus().isAdmin()).isTrue();
        assertThat(result.botStatus().canPostMessages()).isTrue();
        assertThat(result.botStatus().missingPermissions()).isEmpty();
        assertThat(result.userStatus().isMember()).isTrue();
        assertThat(result.userStatus().role())
                .isEqualTo("CREATOR");
    }

    @Test
    @DisplayName("Should propagate DomainException from Telegram port "
            + "(no CompletionException wrapping)")
    void shouldPropagateDomainExceptionFromTelegramPort() {
        when(telegramChannel.getChatByUsername(USERNAME))
                .thenReturn(chatInfo("channel"));
        when(telegramChannel.getChatAdministrators(CHANNEL_ID))
                .thenThrow(new DomainException(SERVICE_UNAVAILABLE, "boom"));
        when(telegramChannel.getChatMemberCount(CHANNEL_ID))
                .thenReturn(1000);

        assertThatThrownBy(() -> service.verify(USERNAME, USER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(SERVICE_UNAVAILABLE);
    }

    private static ChatInfo chatInfo(String type) {
        return new ChatInfo(CHANNEL_ID, "Test Channel",
                USERNAME, type, "Test description");
    }

    private static ChatMemberInfo memberInfo(long userId,
                                             ChatMemberStatus status) {
        return new ChatMemberInfo(userId, status,
                false, false, false, false);
    }

    private static ChatMemberInfo adminInfo(long userId) {
        return new ChatMemberInfo(userId, ChatMemberStatus.CREATOR,
                true, true, true, true);
    }

    private static ChatMemberInfo botAdmin() {
        return new ChatMemberInfo(BOT_ID,
                ChatMemberStatus.ADMINISTRATOR,
                true, true, true, true);
    }
}
