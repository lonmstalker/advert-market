package com.advertmarket.deal.service;

import com.advertmarket.deal.api.dto.CreateDealCommand;
import com.advertmarket.deal.api.dto.DealDetailDto;
import com.advertmarket.deal.api.dto.DealDto;
import com.advertmarket.deal.api.dto.DealEventDto;
import com.advertmarket.deal.api.dto.DealEventRecord;
import com.advertmarket.deal.api.dto.DealListCriteria;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.api.port.DealAuthorizationPort;
import com.advertmarket.deal.api.port.DealEventRepository;
import com.advertmarket.deal.api.port.DealPort;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.deal.repository.JooqDealRepository;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.financial.CommissionCalculator;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.pagination.CursorPage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for deal CRUD and transition delegation.
 */
@Service
@RequiredArgsConstructor
public class DealService implements DealPort {

    private static final int DEFAULT_COMMISSION_RATE_BP = 1000;

    private final DealRepository dealRepository;
    private final DealEventRepository dealEventRepository;
    private final DealAuthorizationPort dealAuthorizationPort;
    private final DealTransitionService dealTransitionService;
    private final ChannelRepository channelRepository;

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
        var channel = channelRepository.findDetailById(command.channelId())
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCodes.CHANNEL_NOT_FOUND, "Channel",
                        String.valueOf(command.channelId())));

        var amount = Money.ofNano(command.amountNano());
        var commission = CommissionCalculator.calculate(
                amount, DEFAULT_COMMISSION_RATE_BP);

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
                command.creativeBrief(),
                null, null, null, null, null, null, null,
                null, null, null, null,
                0, now, now);

        dealRepository.insert(record);
        return toDto(record);
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
        return toDetailDto(deal, events);
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

        var items = page.stream().map(this::toDto).toList();
        String nextCursor = hasMore
                ? JooqDealRepository.buildCursor(page.getLast())
                : null;

        return new CursorPage<>(items, nextCursor);
    }

    @Override
    @Transactional
    public @NonNull DealTransitionResult transition(
            @NonNull DealTransitionCommand command) {
        return dealTransitionService.transition(command);
    }

    private DealDto toDto(DealRecord r) {
        return new DealDto(
                DealId.of(r.id()),
                r.channelId(),
                r.advertiserId(),
                r.ownerId(),
                r.status(),
                r.amountNano(),
                r.deadlineAt(),
                r.createdAt(),
                r.version());
    }

    private DealDetailDto toDetailDto(DealRecord r,
                                       List<DealEventRecord> events) {
        var timeline = events.stream().map(this::toEventDto).toList();
        return new DealDetailDto(
                DealId.of(r.id()),
                r.channelId(),
                r.advertiserId(),
                r.ownerId(),
                r.status(),
                r.amountNano(),
                r.commissionRateBp(),
                r.commissionNano(),
                r.deadlineAt(),
                r.createdAt(),
                r.version(),
                timeline);
    }

    private DealEventDto toEventDto(DealEventRecord e) {
        return new DealEventDto(
                e.id() != null ? e.id() : 0L,
                e.eventType(),
                parseStatus(e.fromStatus()),
                parseStatus(e.toStatus()),
                e.actorId(),
                e.createdAt());
    }

    private static DealStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return DealStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
