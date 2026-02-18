package com.advertmarket.deal.service;

import com.advertmarket.deal.api.dto.CreateDealCommand;
import com.advertmarket.deal.api.dto.DealDetailDto;
import com.advertmarket.deal.api.dto.DealDto;
import com.advertmarket.deal.api.dto.DealEventRecord;
import com.advertmarket.deal.api.dto.DealListCriteria;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.api.port.DealAuthorizationPort;
import com.advertmarket.deal.api.port.DealEventRepository;
import com.advertmarket.deal.api.port.DealPort;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.deal.mapper.DealDtoMapper;
import com.advertmarket.deal.repository.JooqDealRepository;
import com.advertmarket.financial.api.port.EscrowPort;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.CreativeRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.financial.CommissionCalculator;
import com.advertmarket.shared.json.JsonException;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.pagination.CursorPage;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for deal CRUD and transition delegation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DealService implements DealPort {

    private static final int DEFAULT_COMMISSION_RATE_BP = 200;
    private static final long MIN_DEAL_AMOUNT_NANO =
            Money.NANO_PER_TON / 2;

    private final DealRepository dealRepository;
    private final DealEventRepository dealEventRepository;
    private final DealAuthorizationPort dealAuthorizationPort;
    private final DealTransitionService dealTransitionService;
    private final ChannelAutoSyncPort channelAutoSyncPort;
    private final ChannelRepository channelRepository;
    private final EscrowPort escrowPort;
    private final CreativeRepository creativeRepository;
    private final DealDtoMapper dealDtoMapper;
    private final JsonFacade jsonFacade;

    @Override
    @Transactional
    public @NonNull DealDto create(@NonNull CreateDealCommand command) {
        throw new UnsupportedOperationException(
                "Use create(command, advertiserId) instead");
    }

    /** Creates a deal on behalf of the advertiser. */
    @Transactional
    public @NonNull DealDto create(@NonNull CreateDealCommand command,
                                   long advertiserId) {
        syncChannelForCreate(command.channelId());
        var channel = channelRepository.findDetailById(command.channelId())
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCodes.CHANNEL_NOT_FOUND, "Channel",
                        String.valueOf(command.channelId())));

        if (!channel.isActive()) {
            throw new DomainException(ErrorCodes.CHANNEL_NOT_ACTIVE,
                    "Channel is not active: " + command.channelId());
        }
        if (command.amountNano() < MIN_DEAL_AMOUNT_NANO) {
            throw new DomainException(
                    ErrorCodes.INVALID_PARAMETER,
                    "Deal amount must be at least "
                            + MIN_DEAL_AMOUNT_NANO + " nanoTON");
        }

        var amount = Money.ofNano(command.amountNano());
        var commission = CommissionCalculator.calculate(
                amount, DEFAULT_COMMISSION_RATE_BP);
        var creativeBrief = resolveCreativeBrief(command, advertiserId);

        var dealId = UUID.randomUUID();
        var now = Instant.now();
        var record = new DealRecord(
                dealId,
                command.channelId(),
                advertiserId,
                channel.ownerId(),
                command.pricingRuleId(),
                DealStatus.DRAFT,
                command.amountNano(),
                DEFAULT_COMMISSION_RATE_BP,
                commission.commission().nanoTon(),
                null, null,
                creativeBrief,
                null, null, null, null, null, null, null,
                null, null, null, null,
                0, now, now);

        dealRepository.insert(record);
        return dealDtoMapper.toDto(record);
    }

    private void syncChannelForCreate(long channelId) {
        syncChannelWithRateLimitFallback(channelId, "create");
    }

    private void syncChannelForTransition(long channelId) {
        syncChannelWithRateLimitFallback(channelId, "transition");
    }

    private void syncChannelWithRateLimitFallback(
            long channelId,
            String operation) {
        try {
            channelAutoSyncPort.syncFromTelegram(channelId);
        } catch (DomainException exception) {
            switch (exception.getErrorCode()) {
                case ErrorCodes.RATE_LIMIT_EXCEEDED -> log.warn(
                        "Channel sync rate-limited for {}; "
                                + "falling back to cached channel snapshot: {}",
                        operation,
                        channelId);
                default -> throw exception;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull DealDetailDto getDetail(@NonNull DealId dealId) {
        if (!dealAuthorizationPort.isParticipant(dealId)) {
            throw new DomainException(ErrorCodes.DEAL_NOT_PARTICIPANT,
                    "Not a participant of deal " + dealId);
        }

        var deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCodes.DEAL_NOT_FOUND, "Deal",
                        dealId.value().toString()));

        var events = dealEventRepository.findByDealId(dealId);
        var timeline = events.stream()
                .map(dealDtoMapper::toEventDto)
                .toList();
        return dealDtoMapper.toDetailDto(deal, timeline);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull CursorPage<DealDto> listForUser(
            @NonNull DealListCriteria criteria) {
        throw new UnsupportedOperationException(
                "Use listForUser(criteria, userId) instead");
    }

    /** Lists deals for the given user with cursor-based pagination. */
    @Transactional(readOnly = true)
    public @NonNull CursorPage<DealDto> listForUser(
            @NonNull DealListCriteria criteria, long userId) {
        int fetchLimit = criteria.limit() + 1;
        var adjusted = new DealListCriteria(
                criteria.status(), criteria.cursor(), fetchLimit);

        var records = dealRepository.listByUser(userId, adjusted);

        boolean hasMore = records.size() > criteria.limit();
        var page = hasMore
                ? records.subList(0, criteria.limit())
                : records;

        var items = page.stream()
                .map(dealDtoMapper::toDto)
                .toList();
        String nextCursor = hasMore
                ? JooqDealRepository.buildCursor(page.getLast())
                : null;

        return new CursorPage<>(items, nextCursor);
    }

    @Override
    @Transactional
    public @NonNull DealTransitionResult transition(
            @NonNull DealTransitionCommand command) {
        reconcileOwnerForTransition(command);
        var result = dealTransitionService.transition(command);
        autoAdvanceToAwaitingPayment(command, result);
        return result;
    }

    private void autoAdvanceToAwaitingPayment(
            DealTransitionCommand command,
            DealTransitionResult result) {
        if (!isOwnerAcceptance(command, result)) {
            return;
        }

        try {
            var autoAdvanceResult = dealTransitionService.transition(
                    new DealTransitionCommand(
                    command.dealId(),
                    DealStatus.AWAITING_PAYMENT,
                    null,
                    ActorType.SYSTEM,
                    "Auto transition after acceptance",
                    null,
                    null));
            bootstrapAwaitingPaymentDeposit(
                    command.dealId(),
                    autoAdvanceResult);
        } catch (DomainException exception) {
            // Keep owner accept committed; retry path remains available via workflow.
            log.error(
                    "Failed to auto-advance deal to AWAITING_PAYMENT after owner acceptance: {}",
                    command.dealId(),
                    exception);
        }
    }

    private static boolean isOwnerAcceptance(
            DealTransitionCommand command,
            DealTransitionResult result) {
        if (command.targetStatus() != DealStatus.ACCEPTED) {
            return false;
        }
        if (command.actorType() != ActorType.CHANNEL_OWNER
                && command.actorType() != ActorType.CHANNEL_ADMIN) {
            return false;
        }
        return result instanceof DealTransitionResult.Success
                || result instanceof DealTransitionResult.AlreadyInTargetState;
    }

    private void bootstrapAwaitingPaymentDeposit(
            DealId dealId,
            DealTransitionResult autoAdvanceResult) {
        if (!(autoAdvanceResult instanceof DealTransitionResult.Success)
                && !(autoAdvanceResult
                instanceof DealTransitionResult.AlreadyInTargetState)) {
            return;
        }

        var currentDeal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCodes.DEAL_NOT_FOUND,
                        "Deal",
                        dealId.value().toString()));

        if (currentDeal.status() != DealStatus.AWAITING_PAYMENT) {
            return;
        }

        String existingAddress = currentDeal.depositAddress();
        Integer existingSubwallet = currentDeal.subwalletId();
        if (existingAddress != null && !existingAddress.isBlank()
                && existingSubwallet != null) {
            return;
        }

        var addressInfo = escrowPort.generateDepositAddress(
                dealId,
                currentDeal.amountNano());
        int subwalletId = Math.toIntExact(addressInfo.subwalletId());
        dealRepository.setDepositAddress(
                dealId,
                addressInfo.depositAddress(),
                subwalletId);
    }

    private void reconcileOwnerForTransition(
            DealTransitionCommand command) {
        if (!requiresLiveSync(command)) {
            return;
        }

        var deal = dealRepository.findById(command.dealId())
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCodes.DEAL_NOT_FOUND,
                        "Deal",
                        command.dealId().value().toString()));

        syncChannelForTransition(deal.channelId());
        var channel = channelRepository.findDetailById(deal.channelId())
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCodes.CHANNEL_NOT_FOUND,
                        "Channel",
                        String.valueOf(deal.channelId())));

        long oldOwnerId = deal.ownerId();
        long newOwnerId = channel.ownerId();
        if (oldOwnerId == newOwnerId || deal.status().isTerminal()) {
            return;
        }

        boolean reassigned = dealRepository.reassignOwnerIfNonTerminal(
                command.dealId(), newOwnerId);
        if (reassigned) {
            appendOwnerReassignmentEvent(deal, command, oldOwnerId, newOwnerId);
        }
    }

    private static boolean requiresLiveSync(DealTransitionCommand command) {
        return command.actorType() == ActorType.CHANNEL_OWNER
                || command.actorType() == ActorType.CHANNEL_ADMIN
                || command.targetStatus() == DealStatus.COMPLETED_RELEASED;
    }

    private @Nullable String resolveCreativeBrief(
            CreateDealCommand command,
            long advertiserId) {
        String creativeId = normalizeOptional(command.creativeId());
        if (creativeId == null) {
            return normalizeManualCreativeBrief(command.creativeBrief());
        }

        var creative = creativeRepository.findByOwnerAndId(advertiserId, creativeId)
                .orElseThrow(() -> new DomainException(
                        ErrorCodes.CREATIVE_NOT_FOUND,
                        "Creative not found: " + creativeId));

        return jsonFacade.toJson(Map.of(
                "creativeId", creative.id(),
                "title", creative.title(),
                "version", creative.version(),
                "draft", creative.draft()));
    }

    private @Nullable String normalizeManualCreativeBrief(
            @Nullable String rawCreativeBrief) {
        String creativeBrief = normalizeOptional(rawCreativeBrief);
        if (creativeBrief == null) {
            return null;
        }

        try {
            jsonFacade.readTree(creativeBrief);
            return creativeBrief;
        } catch (JsonException ignored) {
            return jsonFacade.toJson(Map.of("text", creativeBrief));
        }
    }

    private static @Nullable String normalizeOptional(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @SuppressWarnings("fenum:assignment")
    private void appendOwnerReassignmentEvent(
            DealRecord deal,
            DealTransitionCommand command,
            long oldOwnerId,
            long newOwnerId) {
        String payload = jsonFacade.toJson(Map.of(
                "type", "OWNER_REASSIGNED",
                "oldOwnerId", oldOwnerId,
                "newOwnerId", newOwnerId));
        dealEventRepository.append(new DealEventRecord(
                null,
                deal.id(),
                "AUDIT_EVENT",
                null,
                null,
                command.actorId(),
                command.actorType().name(),
                payload,
                Instant.now()));
    }
}
