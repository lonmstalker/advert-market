package com.advertmarket.deal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.deal.api.dto.CreateDealCommand;
import com.advertmarket.deal.api.dto.DealListCriteria;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.api.port.DealAuthorizationPort;
import com.advertmarket.deal.api.port.DealEventRepository;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.deal.mapper.DealDtoMapper;
import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeDraftDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeTemplateDto;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.CreativeRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.json.JsonException;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("DealService")
@ExtendWith(MockitoExtension.class)
class DealServiceTest {

    @Mock
    private DealRepository dealRepository;
    @Mock
    private DealEventRepository dealEventRepository;
    @Mock
    private DealAuthorizationPort dealAuthorizationPort;
    @Mock
    private DealTransitionService dealTransitionService;
    @Mock
    private ChannelAutoSyncPort channelAutoSyncPort;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private JsonFacade jsonFacade;
    @Mock
    private CreativeRepository creativeRepository;

    private DealService service;

    @BeforeEach
    void setUp() {
        service = new DealService(
                dealRepository, dealEventRepository,
                dealAuthorizationPort, dealTransitionService,
                channelAutoSyncPort, channelRepository,
                creativeRepository, Mappers.getMapper(DealDtoMapper.class), jsonFacade);
    }

    private ChannelDetailResponse channelDetail(long channelId, long ownerId) {
        return new ChannelDetailResponse(
                channelId, "Test Channel", "test", "desc", 1000,
                List.of("crypto"), 1_000_000_000L, true, ownerId,
                BigDecimal.valueOf(5.5), 500, "ru",
                new ChannelDetailResponse.ChannelRules(null),
                List.<PricingRuleDto>of(),
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    private DealRecord dealRecord(DealId dealId, long advertiserId, long ownerId) {
        return new DealRecord(
                dealId.value(), 1L, advertiserId, ownerId, null,
                DealStatus.DRAFT, 1_000_000_000L, 1000, 100_000_000L,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                0, Instant.now(), Instant.now());
    }

    private CreativeTemplateDto creativeTemplate(String creativeId) {
        return new CreativeTemplateDto(
                creativeId,
                "Native launch template",
                new CreativeDraftDto(
                        "Install app",
                        List.of(),
                        List.of(),
                        List.of(),
                        false),
                3,
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should create deal in DRAFT status with commission calculated")
        void create_shouldCreateDeal() {
            var cmd = new CreateDealCommand(1L, 1_000_000_000L, null, null, null);
            when(channelRepository.findDetailById(1L))
                    .thenReturn(Optional.of(channelDetail(1L, 200L)));

            var result = service.create(cmd, 100L);

            assertThat(result.status()).isEqualTo(DealStatus.DRAFT);
            assertThat(result.channelId()).isEqualTo(1L);
            assertThat(result.advertiserId()).isEqualTo(100L);
            assertThat(result.ownerId()).isEqualTo(200L);
            assertThat(result.amountNano()).isEqualTo(1_000_000_000L);

            var captor = ArgumentCaptor.forClass(DealRecord.class);
            verify(dealRepository).insert(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.status()).isEqualTo(DealStatus.DRAFT);
            assertThat(saved.commissionRateBp()).isEqualTo(200);
            assertThat(saved.commissionNano()).isEqualTo(20_000_000L);
        }

        @Test
        @DisplayName("should throw when channel not found")
        void create_channelNotFound_shouldThrow() {
            var cmd = new CreateDealCommand(999L, 1_000_000_000L, null, null, null);
            when(channelRepository.findDetailById(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(cmd, 100L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should reject deal amount below minimum anti-dust threshold")
        void create_amountBelowMinimum_shouldThrow() {
            var cmd = new CreateDealCommand(1L, 499_999_999L, null, null, null);
            when(channelRepository.findDetailById(1L))
                    .thenReturn(Optional.of(channelDetail(1L, 200L)));

            assertThatThrownBy(() -> service.create(cmd, 100L))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.INVALID_PARAMETER);
        }

        @Test
        @DisplayName("should persist creative snapshot when command references creativeId")
        void create_withCreativeId_shouldPersistCreativeSnapshot() {
            when(channelRepository.findDetailById(1L))
                    .thenReturn(Optional.of(channelDetail(1L, 200L)));
            when(creativeRepository.findByOwnerAndId(100L, "creative-42"))
                    .thenReturn(Optional.of(creativeTemplate("creative-42")));
            when(jsonFacade.toJson(any())).thenReturn("{\"creativeId\":\"creative-42\"}");

            var cmd = new CreateDealCommand(1L, 1_000_000_000L, null, null, "creative-42");
            service.create(cmd, 100L);

            var captor = ArgumentCaptor.forClass(DealRecord.class);
            verify(dealRepository).insert(captor.capture());
            assertThat(captor.getValue().creativeBrief())
                    .isEqualTo("{\"creativeId\":\"creative-42\"}");
        }

        @Test
        @DisplayName("should fail with CREATIVE_NOT_FOUND when creativeId is unknown")
        void create_withUnknownCreativeId_shouldThrow() {
            var cmd = new CreateDealCommand(1L, 1_000_000_000L, null, null, "creative-missing");
            when(channelRepository.findDetailById(1L))
                    .thenReturn(Optional.of(channelDetail(1L, 200L)));
            when(creativeRepository.findByOwnerAndId(100L, "creative-missing"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(cmd, 100L))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.CREATIVE_NOT_FOUND);
        }

        @Test
        @DisplayName("should create deal using cached channel when live sync is rate-limited")
        void create_whenSyncRateLimited_shouldFallbackToCachedChannel() {
            var cmd = new CreateDealCommand(1L, 1_000_000_000L, null, null, null);
            when(channelAutoSyncPort.syncFromTelegram(1L))
                    .thenThrow(new DomainException(
                            ErrorCodes.RATE_LIMIT_EXCEEDED,
                            "rate limited"));
            when(channelRepository.findDetailById(1L))
                    .thenReturn(Optional.of(channelDetail(1L, 200L)));

            var result = service.create(cmd, 100L);

            assertThat(result.status()).isEqualTo(DealStatus.DRAFT);
            verify(dealRepository).insert(any(DealRecord.class));
        }

        @Test
        @DisplayName("should wrap plain-text creativeBrief into JSON payload")
        void create_withPlainTextCreativeBrief_shouldPersistJson() {
            when(channelRepository.findDetailById(1L))
                    .thenReturn(Optional.of(channelDetail(1L, 200L)));
            when(jsonFacade.readTree("Need native integration"))
                    .thenThrow(new JsonException("invalid json"));
            when(jsonFacade.toJson(any()))
                    .thenReturn("{\"text\":\"Need native integration\"}");

            var cmd = new CreateDealCommand(
                    1L,
                    1_000_000_000L,
                    null,
                    "Need native integration",
                    null);
            service.create(cmd, 100L);

            var captor = ArgumentCaptor.forClass(DealRecord.class);
            verify(dealRepository).insert(captor.capture());
            assertThat(captor.getValue().creativeBrief())
                    .isEqualTo("{\"text\":\"Need native integration\"}");
        }
    }

    @Nested
    @DisplayName("getDetail()")
    class GetDetail {

        @Test
        @DisplayName("should return deal detail with timeline for participant")
        void getDetail_participant_shouldReturnDetail() {
            var dealId = DealId.generate();
            var deal = dealRecord(dealId, 100L, 200L);
            when(dealAuthorizationPort.isParticipant(dealId)).thenReturn(true);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
            when(dealEventRepository.findByDealId(dealId)).thenReturn(List.of());

            var result = service.getDetail(dealId);

            assertThat(result.id()).isEqualTo(dealId);
            assertThat(result.timeline()).isEmpty();
        }

        @Test
        @DisplayName("should throw when not participant")
        void getDetail_notParticipant_shouldThrow() {
            var dealId = DealId.generate();
            when(dealAuthorizationPort.isParticipant(dealId)).thenReturn(false);

            assertThatThrownBy(() -> service.getDetail(dealId))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.DEAL_NOT_PARTICIPANT);
        }
    }

    @Nested
    @DisplayName("listForUser()")
    class ListForUser {

        @Test
        @DisplayName("should return cursor page for user deals")
        void listForUser_shouldReturnPage() {
            var dealId = DealId.generate();
            var deal = dealRecord(dealId, 100L, 200L);
            var criteria = new DealListCriteria(null, null, 20);
            when(dealRepository.listByUser(eq(100L), any()))
                    .thenReturn(List.of(deal));

            var result = service.listForUser(criteria, 100L);

            assertThat(result.items()).hasSize(1);
            assertThat(result.hasMore()).isFalse();
        }
    }

    @Nested
    @DisplayName("transition()")
    class Transition {

        @Test
        @DisplayName("should delegate to DealTransitionService")
        void transition_shouldDelegate() {
            var cmd = new DealTransitionCommand(
                    DealId.generate(), DealStatus.OFFER_PENDING,
                    100L, ActorType.ADVERTISER, null, null, null);
            var expected = new DealTransitionResult.Success(DealStatus.OFFER_PENDING);
            when(dealTransitionService.transition(cmd)).thenReturn(expected);

            var result = service.transition(cmd);

            assertThat(result).isEqualTo(expected);
        }
    }
}
