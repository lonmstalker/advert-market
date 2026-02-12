package com.advertmarket.marketplace.service;

import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_ALREADY_REGISTERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.api.channel.ChatInfo;
import com.advertmarket.marketplace.api.dto.ChannelRegistrationRequest;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse.BotStatus;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse.UserStatus;
import com.advertmarket.marketplace.api.dto.NewChannel;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.shared.exception.DomainException;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ChannelRegistrationService — channel registration flow")
@ExtendWith(MockitoExtension.class)
class ChannelRegistrationServiceTest {

    private static final long CHANNEL_ID = -1001234567890L;
    private static final long USER_ID = 222L;
    private static final String USERNAME = "testchannel";

    @Mock
    private ChannelVerificationService verificationService;

    @Mock
    private ChannelRepository channelRepository;

    @InjectMocks
    private ChannelRegistrationService service;

    @Test
    @DisplayName("Should delegate verification to ChannelVerificationService")
    void shouldDelegateVerify() {
        var expected = verifyResponse();
        when(verificationService.verify(USERNAME, USER_ID))
                .thenReturn(expected);

        var result = service.verify(USERNAME, USER_ID);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("Should throw CHANNEL_ALREADY_REGISTERED for duplicate")
    void shouldRejectDuplicateChannel() {
        var request = new ChannelRegistrationRequest(
                CHANNEL_ID, "tech", null);
        when(channelRepository.existsByTelegramId(CHANNEL_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.register(request, USER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(CHANNEL_ALREADY_REGISTERED);

        verify(channelRepository, never())
                .insert(any(NewChannel.class));
    }

    @Test
    @DisplayName("Should register channel on happy path")
    void shouldRegisterSuccessfully() {
        var request = new ChannelRegistrationRequest(
                CHANNEL_ID, "tech", 1_000_000_000L);
        when(channelRepository.existsByTelegramId(CHANNEL_ID))
                .thenReturn(false);
        when(verificationService.resolveChannelById(CHANNEL_ID))
                .thenReturn(chatInfo());
        when(verificationService.verifyBotAndUser(any(), eq(USER_ID)))
                .thenReturn(verifyResponse());

        var expected = channelResponse();
        when(channelRepository.insert(any(NewChannel.class)))
                .thenReturn(expected);

        var result = service.register(request, USER_ID);

        assertThat(result.id()).isEqualTo(CHANNEL_ID);
        assertThat(result.title()).isEqualTo("Test Channel");
        assertThat(result.ownerId()).isEqualTo(USER_ID);
    }

    private static ChannelVerifyResponse verifyResponse() {
        return new ChannelVerifyResponse(
                CHANNEL_ID, "Test Channel", USERNAME, 5000,
                new BotStatus(true, true, true, List.of()),
                new UserStatus(true, "CREATOR"));
    }

    private static ChatInfo chatInfo() {
        return new ChatInfo(CHANNEL_ID, "Test Channel",
                USERNAME, "channel", "Test description");
    }

    private static ChannelResponse channelResponse() {
        return new ChannelResponse(
                CHANNEL_ID, "Test Channel", USERNAME,
                null, 5000, "tech",
                1_000_000_000L, true, USER_ID,
                OffsetDateTime.now());
    }
}
