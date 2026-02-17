package com.advertmarket.deal.usecase;

import static com.advertmarket.shared.exception.ErrorCodes.DEAL_NOT_PARTICIPANT;
import static com.advertmarket.shared.exception.ErrorCodes.SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.deal.api.dto.DealDto;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.api.port.DealAuthorizationPort;
import com.advertmarket.deal.service.DealService;
import com.advertmarket.deal.web.CreateDealRequest;
import com.advertmarket.deal.web.DealTransitionRequest;
import com.advertmarket.financial.api.model.DepositInfo;
import com.advertmarket.financial.api.model.DepositStatus;
import com.advertmarket.financial.api.port.DepositPort;
import com.advertmarket.marketplace.api.model.ChannelRight;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.security.PrincipalAuthentication;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("DealUseCase — actor resolution with live sync + ABAC")
@ExtendWith(MockitoExtension.class)
class DealUseCaseTest {

    private static final long USER_ID = 100L;
    private static final long CHANNEL_ID = -100L;

    @Mock
    private DealService dealService;
    @Mock
    private DealAuthorizationPort dealAuthorizationPort;
    @Mock
    private ChannelAutoSyncPort channelAutoSyncPort;
    @Mock
    private ChannelAuthorizationPort channelAuthorizationPort;
    @Mock
    private DepositPort depositPort;

    @InjectMocks
    private DealUseCase useCase;

    @BeforeEach
    void setUp() {
        setCurrentUser(USER_ID, false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("create should forward creativeId from request to service command")
    void create_withCreativeId_forwardsCommandField() {
        var request = new CreateDealRequest(10L, 1_000_000_000L, 5L, null, "creative-42");
        var created = new DealDto(
                DealId.generate(),
                10L,
                USER_ID,
                200L,
                DealStatus.DRAFT,
                1_000_000_000L,
                null,
                Instant.now(),
                1);
        when(dealService.create(any(), eq(USER_ID))).thenReturn(created);

        var response = useCase.create(request);

        assertThat(response).isEqualTo(created);
        var commandCaptor = ArgumentCaptor.forClass(com.advertmarket.deal.api.dto.CreateDealCommand.class);
        verify(dealService).create(commandCaptor.capture(), eq(USER_ID));
        assertThat(commandCaptor.getValue().creativeId()).isEqualTo("creative-42");
    }

    @Test
    @DisplayName("Owner-side transition should use CHANNEL_OWNER after live sync")
    void transition_ownerSideAsOwner() {
        UUID dealUuid = UUID.randomUUID();
        when(dealAuthorizationPort.getChannelId(any())).thenReturn(CHANNEL_ID);
        when(dealAuthorizationPort.isAdvertiser(any())).thenReturn(false);
        when(channelAuthorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
        when(dealService.transition(any())).thenReturn(
                new DealTransitionResult.Success(DealStatus.ACCEPTED));

        var response = useCase.transition(
                dealUuid,
                new DealTransitionRequest(
                        DealStatus.ACCEPTED, null, null, null));

        assertThat(response.status()).isEqualTo("SUCCESS");
        var commandCaptor = ArgumentCaptor.forClass(
                com.advertmarket.deal.api.dto.DealTransitionCommand.class);
        verify(dealService).transition(commandCaptor.capture());
        assertThat(commandCaptor.getValue().actorType())
                .isEqualTo(ActorType.CHANNEL_OWNER);
        verify(channelAutoSyncPort).syncFromTelegram(CHANNEL_ID);
    }

    @Test
    @DisplayName("Owner-side transition should allow manager with mapped right")
    void transition_ownerSideAsManager() {
        UUID dealUuid = UUID.randomUUID();
        when(dealAuthorizationPort.getChannelId(any())).thenReturn(CHANNEL_ID);
        when(dealAuthorizationPort.isAdvertiser(any())).thenReturn(false);
        when(channelAuthorizationPort.isOwner(CHANNEL_ID)).thenReturn(false);
        when(channelAuthorizationPort.hasRight(CHANNEL_ID, ChannelRight.MODERATE))
                .thenReturn(true);
        when(dealService.transition(any())).thenReturn(
                new DealTransitionResult.Success(DealStatus.CANCELLED));

        var response = useCase.transition(
                dealUuid,
                new DealTransitionRequest(
                        DealStatus.CANCELLED, "test", null, null));

        assertThat(response.status()).isEqualTo("SUCCESS");
        var commandCaptor = ArgumentCaptor.forClass(
                com.advertmarket.deal.api.dto.DealTransitionCommand.class);
        verify(dealService).transition(commandCaptor.capture());
        assertThat(commandCaptor.getValue().actorType())
                .isEqualTo(ActorType.CHANNEL_ADMIN);
        verify(channelAutoSyncPort).syncFromTelegram(CHANNEL_ID);
    }

    @Test
    @DisplayName("Old owner should be rejected after transfer if no live ABAC rights")
    void transition_oldOwnerRejectedAfterTransfer() {
        UUID dealUuid = UUID.randomUUID();
        when(dealAuthorizationPort.getChannelId(any())).thenReturn(CHANNEL_ID);
        when(dealAuthorizationPort.isAdvertiser(any())).thenReturn(false);
        when(channelAuthorizationPort.isOwner(CHANNEL_ID)).thenReturn(false);
        when(channelAuthorizationPort.hasRight(CHANNEL_ID, ChannelRight.MODERATE))
                .thenReturn(false);

        assertThatThrownBy(() -> useCase.transition(
                dealUuid,
                new DealTransitionRequest(
                        DealStatus.ACCEPTED, null, null, null)))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(DEAL_NOT_PARTICIPANT);

        verify(channelAutoSyncPort).syncFromTelegram(CHANNEL_ID);
        verify(dealService, never()).transition(any());
    }

    @Test
    @DisplayName("Owner-side transition should fail-closed when Telegram is unavailable")
    void transition_ownerSideFailClosedOnSyncError() {
        UUID dealUuid = UUID.randomUUID();
        when(dealAuthorizationPort.getChannelId(any())).thenReturn(CHANNEL_ID);
        when(dealAuthorizationPort.isAdvertiser(any())).thenReturn(false);
        when(channelAutoSyncPort.syncFromTelegram(CHANNEL_ID))
                .thenThrow(new DomainException(SERVICE_UNAVAILABLE, "down"));

        assertThatThrownBy(() -> useCase.transition(
                dealUuid,
                new DealTransitionRequest(
                        DealStatus.ACCEPTED, null, null, null)))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(SERVICE_UNAVAILABLE);

        verify(dealService, never()).transition(any());
    }

    @Test
    @DisplayName("Advertiser transition should not require Telegram live sync")
    void transition_advertiserNoSync() {
        UUID dealUuid = UUID.randomUUID();
        when(dealAuthorizationPort.isAdvertiser(any())).thenReturn(true);
        when(dealService.transition(any())).thenReturn(
                new DealTransitionResult.Success(DealStatus.OFFER_PENDING));

        var response = useCase.transition(
                dealUuid,
                new DealTransitionRequest(
                        DealStatus.OFFER_PENDING, null, null, null));

        assertThat(response.status()).isEqualTo("SUCCESS");
        verify(channelAutoSyncPort, never()).syncFromTelegram(anyLong());
    }

    @Test
    @DisplayName("getDepositInfo should return financial projection for participant")
    void getDepositInfo_forParticipant() {
        UUID dealUuid = UUID.randomUUID();
        var dealId = com.advertmarket.shared.model.DealId.of(dealUuid);
        when(dealAuthorizationPort.isParticipant(dealId)).thenReturn(true);
        when(depositPort.getDepositInfo(dealId)).thenReturn(
                java.util.Optional.of(new DepositInfo(
                        "UQ_test",
                        "1000000000",
                        dealUuid.toString(),
                        DepositStatus.AWAITING_PAYMENT,
                        null,
                        null,
                        null,
                        null,
                        null)));

        var result = useCase.getDepositInfo(dealUuid);

        assertThat(result.escrowAddress()).isEqualTo("UQ_test");
        assertThat(result.status()).isEqualTo(DepositStatus.AWAITING_PAYMENT);
    }

    @Test
    @DisplayName("approveDeposit should reject non-operator")
    void approveDeposit_nonOperator_rejected() {
        UUID dealUuid = UUID.randomUUID();
        setCurrentUser(USER_ID, false);

        assertThatThrownBy(() -> useCase.approveDeposit(dealUuid))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(
                        com.advertmarket.shared.exception.ErrorCodes.AUTH_INSUFFICIENT_PERMISSIONS);
    }

    private static void setCurrentUser(long userId, boolean operator) {
        SecurityContextHolder.getContext().setAuthentication(
                new PrincipalAuthentication() {
                    @Override
                    public UserId getUserId() {
                        return new UserId(userId);
                    }

                    @Override
                    public String getJti() {
                        return "test-jti";
                    }

                    @Override
                    public boolean isOperator() {
                        return operator;
                    }

                    @Override
                    public long getTokenExpSeconds() {
                        return Long.MAX_VALUE;
                    }

                    @Override
                    public Collection<? extends GrantedAuthority>
                            getAuthorities() {
                        return java.util.List.of();
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
                        return new UserId(userId);
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
                        return String.valueOf(userId);
                    }
                });
    }
}
