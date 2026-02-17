package com.advertmarket.deal.usecase;

import com.advertmarket.deal.api.dto.CreateDealCommand;
import com.advertmarket.deal.api.dto.DealDetailDto;
import com.advertmarket.deal.api.dto.DealDto;
import com.advertmarket.deal.api.dto.DealListCriteria;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.api.port.DealAuthorizationPort;
import com.advertmarket.deal.service.DealService;
import com.advertmarket.deal.web.CreateDealRequest;
import com.advertmarket.deal.web.DealTransitionRequest;
import com.advertmarket.deal.web.DealTransitionResponse;
import com.advertmarket.financial.api.model.DepositInfo;
import com.advertmarket.financial.api.port.DepositPort;
import com.advertmarket.marketplace.api.model.ChannelRight;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.security.SecurityContextUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

/**
 * Orchestrates deal web flows: auth context, command building, service calls.
 *
 * <p>Kept outside {@code ..web..} to keep controllers thin.
 */
@Service
@RequiredArgsConstructor
public class DealUseCase {

    private final DealService dealService;
    private final DealAuthorizationPort dealAuthorizationPort;
    private final ChannelAutoSyncPort channelAutoSyncPort;
    private final ChannelAuthorizationPort channelAuthorizationPort;
    private final DepositPort depositPort;

    /**
     * Creates a new deal for the current authenticated user.
     */
    @NonNull
    public DealDto create(@NonNull CreateDealRequest request) {
        var userId = SecurityContextUtil.currentUserId().value();
        var command = new CreateDealCommand(
                request.channelId(),
                request.amountNano(),
                request.pricingRuleId(),
                request.creativeBrief(),
                request.creativeId());
        return dealService.create(command, userId);
    }

    /**
     * Loads deal details by id.
     */
    @NonNull
    public DealDetailDto getDetail(@NonNull UUID id) {
        return dealService.getDetail(DealId.of(id));
    }

    /**
     * Lists deals for the current authenticated user.
     */
    @NonNull
    public CursorPage<DealDto> list(
            DealStatus status,
            String cursor,
            int limit) {
        var userId = SecurityContextUtil.currentUserId().value();
        var criteria = new DealListCriteria(status, cursor, limit);
        return dealService.listForUser(criteria, userId);
    }

    /**
     * Performs deal status transition as the current actor (advertiser/owner/operator/system).
     */
    @NonNull
    public DealTransitionResponse transition(
            @NonNull UUID id,
            @NonNull DealTransitionRequest request) {
        var dealId = DealId.of(id);
        var actorType = resolveActorType(dealId);
        Long actorId = actorType == ActorType.SYSTEM
                ? null
                : SecurityContextUtil.currentUserId().value();

        var command = new DealTransitionCommand(
                dealId,
                request.targetStatus(),
                actorId,
                actorType,
                request.reason(),
                request.partialRefundNano(),
                request.partialPayoutNano());

        var result = dealService.transition(command);
        return toResponse(result);
    }

    /**
     * Returns deposit projection for a deal.
     */
    @NonNull
    public DepositInfo getDepositInfo(@NonNull UUID id) {
        var dealId = DealId.of(id);
        if (!SecurityContextUtil.isOperator()
                && !dealAuthorizationPort.isParticipant(dealId)) {
            throw new DomainException(
                    ErrorCodes.DEAL_NOT_PARTICIPANT,
                    "Not a participant of deal " + dealId);
        }
        return depositPort.getDepositInfo(dealId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCodes.ENTITY_NOT_FOUND,
                        "Deposit",
                        dealId.value().toString()));
    }

    /**
     * Operator-only approval for deposits awaiting manual review.
     */
    @NonNull
    public DepositInfo approveDeposit(@NonNull UUID id) {
        assertOperator();
        var dealId = DealId.of(id);
        depositPort.approveDeposit(dealId);
        return getDepositInfo(id);
    }

    /**
     * Operator-only rejection for deposits awaiting manual review.
     */
    @NonNull
    public DepositInfo rejectDeposit(@NonNull UUID id) {
        assertOperator();
        var dealId = DealId.of(id);
        depositPort.rejectDeposit(dealId);
        return getDepositInfo(id);
    }

    private ActorType resolveActorType(DealId dealId) {
        long channelId = dealAuthorizationPort.getChannelId(dealId);

        if (SecurityContextUtil.isOperator()) {
            return ActorType.PLATFORM_OPERATOR;
        }
        if (dealAuthorizationPort.isAdvertiser(dealId)) {
            return ActorType.ADVERTISER;
        }

        channelAutoSyncPort.syncFromTelegram(channelId);

        if (channelAuthorizationPort.isOwner(channelId)) {
            return ActorType.CHANNEL_OWNER;
        }
        if (channelAuthorizationPort.hasRight(channelId, ChannelRight.MODERATE)) {
            return ActorType.CHANNEL_ADMIN;
        }
        throw new DomainException(ErrorCodes.DEAL_NOT_PARTICIPANT,
                "User is not a participant of deal " + dealId);
    }

    private static DealTransitionResponse toResponse(
            DealTransitionResult result) {
        return switch (result) {
            case null -> throw new IllegalArgumentException(
                    "Transition result must not be null");
            case DealTransitionResult.Success s ->
                    new DealTransitionResponse(
                            "SUCCESS", s.newStatus(), null);
            case DealTransitionResult.AlreadyInTargetState a ->
                    new DealTransitionResponse(
                            "ALREADY_IN_TARGET_STATE",
                            null,
                            a.currentStatus());
        };
    }

    private static void assertOperator() {
        if (!SecurityContextUtil.isOperator()) {
            throw new DomainException(
                    ErrorCodes.AUTH_INSUFFICIENT_PERMISSIONS,
                    "Operator role required");
        }
    }
}
