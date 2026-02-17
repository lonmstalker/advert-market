package com.advertmarket.deal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.deal.api.dto.DealEventRecord;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.api.port.DealEventRepository;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.exception.InvalidStateTransitionException;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.outbox.OutboxRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("DealTransitionService")
@ExtendWith(MockitoExtension.class)
class DealTransitionServiceTest {

    @Mock
    private DealRepository dealRepository;
    @Mock
    private DealEventRepository dealEventRepository;
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private JsonFacade jsonFacade;

    private DealTransitionService service;

    @BeforeEach
    void setUp() {
        service = new DealTransitionService(
                dealRepository, dealEventRepository, outboxRepository, jsonFacade);
    }

    private DealRecord dealInStatus(DealId dealId, DealStatus status, int version) {
        return new DealRecord(
                dealId.value(), 1L, 100L, 200L, null,
                status, 1_000_000_000L, 1000, 100_000_000L,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                version, Instant.now(), Instant.now());
    }

    private DealTransitionCommand cmd(DealId dealId, DealStatus target,
                                      ActorType actorType) {
        Long actorId = actorType == ActorType.SYSTEM ? null : 100L;
        Long partialRefund = target == DealStatus.PARTIALLY_REFUNDED
                ? 600_000_000L
                : null;
        Long partialPayout = target == DealStatus.PARTIALLY_REFUNDED
                ? 400_000_000L
                : null;
        return new DealTransitionCommand(
                dealId,
                target,
                actorId,
                actorType,
                null,
                partialRefund,
                partialPayout);
    }

    @Nested
    @DisplayName("Happy path transitions")
    class HappyPath {

        @Test
        @DisplayName("transition DRAFT → OFFER_PENDING by ADVERTISER should succeed")
        void transitionDraftToOfferPendingByAdvertiserShouldSucceed() {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, DealStatus.DRAFT, 0);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
            when(dealRepository.updateStatus(dealId, DealStatus.DRAFT,
                    DealStatus.OFFER_PENDING, 0)).thenReturn(1);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            var result = service.transition(
                    cmd(dealId, DealStatus.OFFER_PENDING, ActorType.ADVERTISER));

            assertThat(result).isInstanceOf(DealTransitionResult.Success.class);
            var success = (DealTransitionResult.Success) result;
            assertThat(success.newStatus()).isEqualTo(DealStatus.OFFER_PENDING);
            verify(dealEventRepository).append(any(DealEventRecord.class));
            verify(outboxRepository).save(any());
        }

        @Test
        @DisplayName("transition OFFER_PENDING → ACCEPTED by CHANNEL_OWNER should succeed")
        void transitionOfferPendingToAcceptedByOwnerShouldSucceed() {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, DealStatus.OFFER_PENDING, 1);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
            when(dealRepository.updateStatus(dealId, DealStatus.OFFER_PENDING,
                    DealStatus.ACCEPTED, 1)).thenReturn(1);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            var result = service.transition(
                    cmd(dealId, DealStatus.ACCEPTED, ActorType.CHANNEL_OWNER));

            assertThat(result).isInstanceOf(DealTransitionResult.Success.class);
        }
    }

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("transition DRAFT → FUNDED by ADVERTISER should reject (invalid graph)")
        void transitionDraftToFundedByAdvertiserShouldReject() {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, DealStatus.DRAFT, 0);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

            assertThatThrownBy(() -> service.transition(
                    cmd(dealId, DealStatus.FUNDED, ActorType.ADVERTISER)))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("DISPUTED → PARTIALLY_REFUNDED without amounts should reject")
        void transitionDisputedToPartiallyRefundedWithoutAmountsShouldReject() {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, DealStatus.DISPUTED, 0);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

            var command = new DealTransitionCommand(
                    dealId,
                    DealStatus.PARTIALLY_REFUNDED,
                    1L,
                    ActorType.PLATFORM_OPERATOR,
                    "operator resolution",
                    null,
                    null);

            assertThatThrownBy(() -> service.transition(command))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.MISSING_REQUIRED_FIELD);
        }

        @ParameterizedTest(name = "from terminal state {0} should reject")
        @DisplayName("transition from terminal state should reject")
        @EnumSource(value = DealStatus.class, names = {
                "COMPLETED_RELEASED", "CANCELLED", "REFUNDED",
                "PARTIALLY_REFUNDED", "EXPIRED"})
        void transition_fromTerminal_shouldReject(DealStatus terminal) {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, terminal, 5);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

            assertThatThrownBy(() -> service.transition(
                    cmd(dealId, DealStatus.DRAFT, ActorType.SYSTEM)))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Actor permission checks")
    class ActorPermissions {

        @Test
        @DisplayName("transition DRAFT → OFFER_PENDING by SYSTEM should reject (wrong actor)")
        void transitionDraftToOfferPendingBySystemShouldReject() {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, DealStatus.DRAFT, 0);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

            assertThatThrownBy(() -> service.transition(
                    cmd(dealId, DealStatus.OFFER_PENDING, ActorType.SYSTEM)))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.DEAL_ACTOR_NOT_ALLOWED);
        }

        @Test
        @DisplayName("transition DISPUTED → COMPLETED_RELEASED by ADVERTISER should reject")
        void transitionDisputedToCompletedReleasedByAdvertiserShouldReject() {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, DealStatus.DISPUTED, 3);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

            assertThatThrownBy(() -> service.transition(
                    cmd(dealId, DealStatus.COMPLETED_RELEASED, ActorType.ADVERTISER)))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.DEAL_ACTOR_NOT_ALLOWED);
        }
    }

    @Nested
    @DisplayName("Idempotency")
    class Idempotency {

        @Test
        @DisplayName("transition to current status should return AlreadyInTargetState")
        void transition_idempotent_shouldReturnAlreadyInTargetState() {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, DealStatus.OFFER_PENDING, 2);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

            var result = service.transition(
                    cmd(dealId, DealStatus.OFFER_PENDING, ActorType.ADVERTISER));

            assertThat(result).isInstanceOf(DealTransitionResult.AlreadyInTargetState.class);
            var already = (DealTransitionResult.AlreadyInTargetState) result;
            assertThat(already.currentStatus()).isEqualTo(DealStatus.OFFER_PENDING);
        }
    }

    @Nested
    @DisplayName("Optimistic locking")
    class OptimisticLocking {

        @Test
        @DisplayName("CAS conflict with concurrent transition to same target returns "
                + "AlreadyInTargetState")
        void transition_casConflict_reReadIdempotent_shouldReturnAlreadyInTargetState() {
            var dealId = DealId.generate();
            var dealV0 = dealInStatus(dealId, DealStatus.DRAFT, 0);
            var dealV1 = dealInStatus(dealId, DealStatus.OFFER_PENDING, 1);

            when(dealRepository.findById(dealId))
                    .thenReturn(Optional.of(dealV0))
                    .thenReturn(Optional.of(dealV1));
            when(dealRepository.updateStatus(dealId, DealStatus.DRAFT,
                    DealStatus.OFFER_PENDING, 0)).thenReturn(0);

            var result = service.transition(
                    cmd(dealId, DealStatus.OFFER_PENDING, ActorType.ADVERTISER));

            assertThat(result).isInstanceOf(DealTransitionResult.AlreadyInTargetState.class);
        }

        @Test
        @DisplayName("CAS conflict with concurrent transition to different target should throw")
        void transition_casConflict_reReadDifferent_shouldThrow() {
            var dealId = DealId.generate();
            var dealV0 = dealInStatus(dealId, DealStatus.DRAFT, 0);
            var dealV1 = dealInStatus(dealId, DealStatus.CANCELLED, 1);

            when(dealRepository.findById(dealId))
                    .thenReturn(Optional.of(dealV0))
                    .thenReturn(Optional.of(dealV1));
            when(dealRepository.updateStatus(dealId, DealStatus.DRAFT,
                    DealStatus.OFFER_PENDING, 0)).thenReturn(0);

            assertThatThrownBy(() -> service.transition(
                    cmd(dealId, DealStatus.OFFER_PENDING, ActorType.ADVERTISER)))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Deal not found")
    class NotFound {

        @Test
        @DisplayName("transition for non-existent deal should throw EntityNotFoundException")
        void transition_dealNotFound_shouldThrow() {
            var dealId = DealId.generate();
            when(dealRepository.findById(dealId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.transition(
                    cmd(dealId, DealStatus.OFFER_PENDING, ActorType.ADVERTISER)))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Event and outbox side effects")
    class SideEffects {

        @Test
        @DisplayName("successful transition should append event and create outbox entry")
        void transition_success_shouldAppendEventAndOutbox() {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, DealStatus.ACCEPTED, 2);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
            when(dealRepository.updateStatus(dealId, DealStatus.ACCEPTED,
                    DealStatus.AWAITING_PAYMENT, 2)).thenReturn(1);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            service.transition(
                    cmd(dealId, DealStatus.AWAITING_PAYMENT, ActorType.SYSTEM));

            verify(dealEventRepository).append(any(DealEventRecord.class));
            verify(outboxRepository).save(any());
        }
    }

    @Nested
    @DisplayName("Reason persistence")
    class ReasonPersistence {

        @Test
        @DisplayName("cancellation with reason should store cancellation reason on deal")
        void transition_cancelWithReason_shouldStoreCancellationReason() {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, DealStatus.DRAFT, 0);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
            when(dealRepository.updateStatus(dealId, DealStatus.DRAFT,
                    DealStatus.CANCELLED, 0)).thenReturn(1);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            var command = new DealTransitionCommand(
                    dealId, DealStatus.CANCELLED, 100L,
                    ActorType.ADVERTISER, "Changed my mind",
                    null, null);
            service.transition(command);

            verify(dealRepository).setCancellationReason(
                    dealId, "Changed my mind");
        }

        @Test
        @DisplayName("cancellation without reason should not call setCancellationReason")
        void transition_cancelWithoutReason_shouldNotCallSetCancellationReason() {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, DealStatus.DRAFT, 0);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
            when(dealRepository.updateStatus(dealId, DealStatus.DRAFT,
                    DealStatus.CANCELLED, 0)).thenReturn(1);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            var command = new DealTransitionCommand(
                    dealId, DealStatus.CANCELLED, 100L,
                    ActorType.ADVERTISER, null, null, null);
            service.transition(command);

            verify(dealRepository, never()).setCancellationReason(any(), any());
        }

        @Test
        @DisplayName("cancellation with reason should include reason in event payload")
        void transition_cancelWithReason_shouldIncludeReasonInEventPayload() {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, DealStatus.DRAFT, 0);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
            when(dealRepository.updateStatus(dealId, DealStatus.DRAFT,
                    DealStatus.CANCELLED, 0)).thenReturn(1);
            when(jsonFacade.toJson(any())).thenAnswer(inv -> {
                Object arg = inv.getArgument(0);
                if (arg instanceof Map<?, ?> map
                        && map.containsKey("reason")) {
                    return "{\"reason\":\"Changed my mind\"}";
                }
                return "{}";
            });

            var command = new DealTransitionCommand(
                    dealId, DealStatus.CANCELLED, 100L,
                    ActorType.ADVERTISER, "Changed my mind",
                    null, null);
            service.transition(command);

            var captor = ArgumentCaptor.forClass(DealEventRecord.class);
            verify(dealEventRepository).append(captor.capture());
            assertThat(captor.getValue().payload()).contains("reason");
            assertThat(captor.getValue().payload())
                    .contains("Changed my mind");
        }
    }

    @Nested
    @DisplayName("Parametrized: all valid transitions")
    class AllValidTransitions {

        private static final Arguments[] VALID_TRANSITIONS = {
                // DRAFT
                Arguments.of(
                        DealStatus.DRAFT,
                        DealStatus.OFFER_PENDING,
                        ActorType.ADVERTISER),
                Arguments.of(
                        DealStatus.DRAFT,
                        DealStatus.CANCELLED,
                        ActorType.ADVERTISER),
                // OFFER_PENDING
                Arguments.of(
                        DealStatus.OFFER_PENDING,
                        DealStatus.NEGOTIATING,
                        ActorType.CHANNEL_OWNER),
                Arguments.of(
                        DealStatus.OFFER_PENDING,
                        DealStatus.ACCEPTED,
                        ActorType.CHANNEL_OWNER),
                Arguments.of(
                        DealStatus.OFFER_PENDING,
                        DealStatus.CANCELLED,
                        ActorType.ADVERTISER),
                Arguments.of(
                        DealStatus.OFFER_PENDING,
                        DealStatus.CANCELLED,
                        ActorType.CHANNEL_OWNER),
                Arguments.of(
                        DealStatus.OFFER_PENDING,
                        DealStatus.EXPIRED,
                        ActorType.SYSTEM),
                // NEGOTIATING
                Arguments.of(
                        DealStatus.NEGOTIATING,
                        DealStatus.ACCEPTED,
                        ActorType.CHANNEL_OWNER),
                Arguments.of(
                        DealStatus.NEGOTIATING,
                        DealStatus.CANCELLED,
                        ActorType.ADVERTISER),
                Arguments.of(
                        DealStatus.NEGOTIATING,
                        DealStatus.CANCELLED,
                        ActorType.CHANNEL_OWNER),
                Arguments.of(
                        DealStatus.NEGOTIATING,
                        DealStatus.EXPIRED,
                        ActorType.SYSTEM),
                // ACCEPTED
                Arguments.of(
                        DealStatus.ACCEPTED,
                        DealStatus.AWAITING_PAYMENT,
                        ActorType.SYSTEM),
                Arguments.of(
                        DealStatus.ACCEPTED,
                        DealStatus.CANCELLED,
                        ActorType.ADVERTISER),
                Arguments.of(
                        DealStatus.ACCEPTED,
                        DealStatus.CANCELLED,
                        ActorType.CHANNEL_OWNER),
                // AWAITING_PAYMENT
                Arguments.of(
                        DealStatus.AWAITING_PAYMENT,
                        DealStatus.FUNDED,
                        ActorType.SYSTEM),
                Arguments.of(
                        DealStatus.AWAITING_PAYMENT,
                        DealStatus.CANCELLED,
                        ActorType.ADVERTISER),
                Arguments.of(
                        DealStatus.AWAITING_PAYMENT,
                        DealStatus.EXPIRED,
                        ActorType.SYSTEM),
                // FUNDED
                Arguments.of(
                        DealStatus.FUNDED,
                        DealStatus.CREATIVE_SUBMITTED,
                        ActorType.CHANNEL_OWNER),
                Arguments.of(
                        DealStatus.FUNDED,
                        DealStatus.CANCELLED,
                        ActorType.ADVERTISER),
                Arguments.of(
                        DealStatus.FUNDED,
                        DealStatus.CANCELLED,
                        ActorType.CHANNEL_OWNER),
                Arguments.of(
                        DealStatus.FUNDED,
                        DealStatus.EXPIRED,
                        ActorType.SYSTEM),
                // CREATIVE_SUBMITTED
                Arguments.of(
                        DealStatus.CREATIVE_SUBMITTED,
                        DealStatus.CREATIVE_APPROVED,
                        ActorType.ADVERTISER),
                Arguments.of(
                        DealStatus.CREATIVE_SUBMITTED,
                        DealStatus.FUNDED,
                        ActorType.ADVERTISER),
                Arguments.of(
                        DealStatus.CREATIVE_SUBMITTED,
                        DealStatus.DISPUTED,
                        ActorType.ADVERTISER),
                Arguments.of(
                        DealStatus.CREATIVE_SUBMITTED,
                        DealStatus.EXPIRED,
                        ActorType.SYSTEM),
                // CREATIVE_APPROVED
                Arguments.of(
                        DealStatus.CREATIVE_APPROVED,
                        DealStatus.SCHEDULED,
                        ActorType.CHANNEL_OWNER),
                Arguments.of(
                        DealStatus.CREATIVE_APPROVED,
                        DealStatus.PUBLISHED,
                        ActorType.CHANNEL_OWNER),
                Arguments.of(
                        DealStatus.CREATIVE_APPROVED,
                        DealStatus.CANCELLED,
                        ActorType.ADVERTISER),
                Arguments.of(
                        DealStatus.CREATIVE_APPROVED,
                        DealStatus.CANCELLED,
                        ActorType.CHANNEL_OWNER),
                Arguments.of(
                        DealStatus.CREATIVE_APPROVED,
                        DealStatus.EXPIRED,
                        ActorType.SYSTEM),
                // SCHEDULED
                Arguments.of(
                        DealStatus.SCHEDULED,
                        DealStatus.PUBLISHED,
                        ActorType.SYSTEM),
                Arguments.of(
                        DealStatus.SCHEDULED,
                        DealStatus.CANCELLED,
                        ActorType.ADVERTISER),
                Arguments.of(
                        DealStatus.SCHEDULED,
                        DealStatus.CANCELLED,
                        ActorType.CHANNEL_OWNER),
                Arguments.of(
                        DealStatus.SCHEDULED,
                        DealStatus.EXPIRED,
                        ActorType.SYSTEM),
                // PUBLISHED
                Arguments.of(
                        DealStatus.PUBLISHED,
                        DealStatus.DELIVERY_VERIFYING,
                        ActorType.SYSTEM),
                // DELIVERY_VERIFYING
                Arguments.of(
                        DealStatus.DELIVERY_VERIFYING,
                        DealStatus.COMPLETED_RELEASED,
                        ActorType.SYSTEM),
                Arguments.of(
                        DealStatus.DELIVERY_VERIFYING,
                        DealStatus.DISPUTED,
                        ActorType.SYSTEM),
                // DISPUTED
                Arguments.of(
                        DealStatus.DISPUTED,
                        DealStatus.COMPLETED_RELEASED,
                        ActorType.PLATFORM_OPERATOR),
                Arguments.of(
                        DealStatus.DISPUTED,
                        DealStatus.REFUNDED,
                        ActorType.PLATFORM_OPERATOR),
                Arguments.of(
                        DealStatus.DISPUTED,
                        DealStatus.PARTIALLY_REFUNDED,
                        ActorType.PLATFORM_OPERATOR)
        };

        static Stream<Arguments> validTransitions() {
            return Stream.of(VALID_TRANSITIONS);
        }

        @ParameterizedTest(name = "{0} → {1} by {2}")
        @DisplayName("valid transition should succeed")
        @MethodSource("validTransitions")
        void validTransition_shouldSucceed(DealStatus from, DealStatus to, ActorType actor) {
            var dealId = DealId.generate();
            var deal = dealInStatus(dealId, from, 0);
            when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
            when(dealRepository.updateStatus(
                    eq(dealId), eq(from), eq(to), eq(0))).thenReturn(1);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            var result = service.transition(cmd(dealId, to, actor));

            assertThat(result).isInstanceOf(DealTransitionResult.Success.class);
            assertThat(((DealTransitionResult.Success) result).newStatus())
                    .isEqualTo(to);
        }
    }
}
