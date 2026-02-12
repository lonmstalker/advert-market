package com.advertmarket.marketplace.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advertmarket.marketplace.api.dto.ChannelRegistrationRequest;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelVerifyRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse.BotStatus;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse.UserStatus;
import com.advertmarket.marketplace.service.ChannelRegistrationService;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.security.PrincipalAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("ChannelController — /api/v1/channels endpoints")
@ExtendWith(MockitoExtension.class)
class ChannelControllerTest {

    private static final long CHANNEL_ID = -1001234567890L;
    private static final long USER_ID = 222L;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ChannelRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChannelController(
                        registrationService))
                .build();

        SecurityContextHolder.getContext()
                .setAuthentication(testAuth());
    }

    @Test
    @DisplayName("Should return 200 with verify response on success")
    void shouldVerifySuccessfully() throws Exception {
        when(registrationService.verify(eq("mychannel"), anyLong()))
                .thenReturn(verifyResponse());

        mockMvc.perform(post("/api/v1/channels/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChannelVerifyRequest("mychannel"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channelId")
                        .value(CHANNEL_ID))
                .andExpect(jsonPath("$.title")
                        .value("Test Channel"))
                .andExpect(jsonPath("$.botStatus.isAdmin")
                        .value(true))
                .andExpect(jsonPath("$.userStatus.role")
                        .value("CREATOR"));
    }

    @Test
    @DisplayName("Should return 400 when channel username is blank")
    void shouldRejectBlankUsername() throws Exception {
        mockMvc.perform(post("/api/v1/channels/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelUsername\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should propagate DomainException from verify")
    void shouldPropagateVerifyException() {
        when(registrationService.verify(any(), anyLong()))
                .thenThrow(new DomainException(
                        ErrorCodes.CHANNEL_BOT_NOT_MEMBER,
                        "Bot is not a member"));

        assertThatThrownBy(() ->
                mockMvc.perform(post("/api/v1/channels/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChannelVerifyRequest("mychannel")))))
                .rootCause()
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CHANNEL_BOT_NOT_MEMBER);
    }

    @Test
    @DisplayName("Should return 201 with channel on registration success")
    void shouldRegisterSuccessfully() throws Exception {
        when(registrationService.register(
                any(ChannelRegistrationRequest.class), anyLong()))
                .thenReturn(channelResponse());

        mockMvc.perform(post("/api/v1/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChannelRegistrationRequest(
                                        CHANNEL_ID, "tech", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CHANNEL_ID))
                .andExpect(jsonPath("$.title")
                        .value("Test Channel"))
                .andExpect(jsonPath("$.ownerId").value(USER_ID));
    }

    @Test
    @DisplayName("Should propagate CHANNEL_ALREADY_REGISTERED")
    void shouldPropagateConflict() {
        when(registrationService.register(any(), anyLong()))
                .thenThrow(new DomainException(
                        ErrorCodes.CHANNEL_ALREADY_REGISTERED,
                        "Already registered"));

        assertThatThrownBy(() ->
                mockMvc.perform(post("/api/v1/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChannelRegistrationRequest(
                                        CHANNEL_ID, null, null)))))
                .rootCause()
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CHANNEL_ALREADY_REGISTERED);
    }

    private static ChannelVerifyResponse verifyResponse() {
        return new ChannelVerifyResponse(
                CHANNEL_ID, "Test Channel", "mychannel", 5000,
                new BotStatus(true, true, true, List.of()),
                new UserStatus(true, "CREATOR"));
    }

    private static ChannelResponse channelResponse() {
        return new ChannelResponse(
                CHANNEL_ID, "Test Channel", "mychannel",
                null, 5000, "tech",
                null, true, USER_ID,
                OffsetDateTime.parse("2026-01-01T00:00:00Z"));
    }

    private static PrincipalAuthentication testAuth() {
        return new PrincipalAuthentication() {
            @Override
            public UserId getUserId() {
                return new UserId(USER_ID);
            }

            @Override
            public String getJti() {
                return "test-jti";
            }

            @Override
            public boolean isOperator() {
                return false;
            }

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of();
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return getUserId();
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) {
            }

            @Override
            public String getName() {
                return String.valueOf(USER_ID);
            }
        };
    }
}
