package com.advertmarket.marketplace.channel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelListItem;
import com.advertmarket.marketplace.api.dto.ChannelRegistrationRequest;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import com.advertmarket.marketplace.api.dto.ChannelSort;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse.BotStatus;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse.UserStatus;
import com.advertmarket.marketplace.channel.service.ChannelRegistrationService;
import com.advertmarket.marketplace.channel.service.ChannelService;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.security.PrincipalAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @Mock
    private ChannelService channelService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChannelController(
                        registrationService, channelService))
                .build();

        SecurityContextHolder.getContext()
                .setAuthentication(testAuth());
    }

    // --- Search ---

    @Test
    @DisplayName("Should return 200 with search results")
    void shouldSearchSuccessfully() throws Exception {
        when(channelService.search(any()))
                .thenReturn(new CursorPage<>(
                        List.of(channelListItem()), null));

        mockMvc.perform(get("/api/v1/channels")
                        .param("sort", "SUBSCRIBERS_DESC")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].id")
                        .value(CHANNEL_ID))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    @DisplayName("Should map legacy aliases for search parameters")
    void shouldMapLegacyAliasesForSearch() throws Exception {
        when(channelService.search(any())).thenReturn(CursorPage.empty());

        mockMvc.perform(get("/api/v1/channels")
                        .param("q", "crypto")
                        .param("minSubs", "1000")
                        .param("maxSubs", "5000")
                        .param("sort", "price_desc")
                        .param("limit", "10"))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(ChannelSearchCriteria.class);
        verify(channelService).search(captor.capture());
        ChannelSearchCriteria criteria = captor.getValue();
        assertThat(criteria.query()).isEqualTo("crypto");
        assertThat(criteria.minSubscribers()).isEqualTo(1000);
        assertThat(criteria.maxSubscribers()).isEqualTo(5000);
        assertThat(criteria.sort()).isEqualTo(ChannelSort.PRICE_DESC);
        assertThat(criteria.limit()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should return channels count and map legacy aliases")
    void shouldReturnCount() throws Exception {
        when(channelService.count(any())).thenReturn(7L);

        mockMvc.perform(get("/api/v1/channels/count")
                        .param("q", "ton")
                        .param("minSubs", "100")
                        .param("maxSubs", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(7));

        var captor = ArgumentCaptor.forClass(ChannelSearchCriteria.class);
        verify(channelService).count(captor.capture());
        ChannelSearchCriteria criteria = captor.getValue();
        assertThat(criteria.query()).isEqualTo("ton");
        assertThat(criteria.minSubscribers()).isEqualTo(100);
        assertThat(criteria.maxSubscribers()).isEqualTo(500);
    }

    // --- Detail ---

    @Test
    @DisplayName("Should return 200 with channel detail")
    void shouldGetDetailSuccessfully() throws Exception {
        when(channelService.getDetail(CHANNEL_ID))
                .thenReturn(channelDetail());

        mockMvc.perform(get("/api/v1/channels/{id}", CHANNEL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CHANNEL_ID))
                .andExpect(jsonPath("$.title")
                        .value("Test Channel"))
                .andExpect(jsonPath("$.pricingRules").isArray());
    }

    @Test
    @DisplayName("Should propagate CHANNEL_NOT_FOUND from detail")
    void shouldPropagateNotFoundFromDetail() {
        when(channelService.getDetail(CHANNEL_ID))
                .thenThrow(new DomainException(
                        ErrorCodes.CHANNEL_NOT_FOUND,
                        "Channel not found"));

        assertThatThrownBy(() ->
                mockMvc.perform(get("/api/v1/channels/{id}",
                        CHANNEL_ID)))
                .rootCause()
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CHANNEL_NOT_FOUND);
    }

    // --- Update ---

    @Test
    @DisplayName("Should return 200 on successful update")
    void shouldUpdateSuccessfully() throws Exception {
        when(channelService.update(eq(CHANNEL_ID), any()))
                .thenReturn(channelResponse());

        mockMvc.perform(put("/api/v1/channels/{id}", CHANNEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChannelUpdateRequest(
                                        "new desc", null,
                                        null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CHANNEL_ID));
    }

    @Test
    @DisplayName("Should propagate CHANNEL_NOT_OWNED from update")
    void shouldPropagateNotOwnedFromUpdate() {
        when(channelService.update(eq(CHANNEL_ID), any()))
                .thenThrow(new DomainException(
                        ErrorCodes.CHANNEL_NOT_OWNED,
                        "Not owner"));

        assertThatThrownBy(() ->
                mockMvc.perform(put("/api/v1/channels/{id}",
                                CHANNEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChannelUpdateRequest(
                                        null, null, null,
                                        null, null)))))
                .rootCause()
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CHANNEL_NOT_OWNED);
    }

    // --- Deactivate ---

    @Test
    @DisplayName("Should return 204 on successful deactivation")
    void shouldDeactivateSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/channels/{id}", CHANNEL_ID))
                .andExpect(status().isNoContent());
    }

    // --- Verify ---

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

    // --- Register ---

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
                                        CHANNEL_ID, List.of("tech"), null))))
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

    // --- Helpers ---

    private static ChannelListItem channelListItem() {
        return new ChannelListItem(
                CHANNEL_ID, "Test Channel", "test",
                List.of("tech"), 5000, 1000,
                BigDecimal.valueOf(3.5), 100_000_000L,
                true, OffsetDateTime.parse("2026-01-01T00:00:00Z"));
    }

    private static ChannelDetailResponse channelDetail() {
        return new ChannelDetailResponse(
                CHANNEL_ID, "Test Channel", "test",
                "Description", 5000, List.of("tech"),
                100_000_000L, true, USER_ID,
                BigDecimal.valueOf(3.5), 1000, "ru",
                List.of(),
                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                OffsetDateTime.parse("2026-01-01T00:00:00Z"));
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
                null, 5000, List.of("tech"),
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
            public long getTokenExpSeconds() {
                return 0;
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
