package com.advertmarket.deal.web;

import com.advertmarket.deal.api.dto.CreateDealCommand;
import com.advertmarket.deal.api.dto.DealDetailDto;
import com.advertmarket.deal.api.dto.DealDto;
import com.advertmarket.deal.api.dto.DealListCriteria;
import com.advertmarket.deal.api.dto.DealTransitionCommand;
import com.advertmarket.deal.api.dto.DealTransitionResult;
import com.advertmarket.deal.api.port.DealAuthorizationPort;
import com.advertmarket.deal.service.DealService;
import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.security.SecurityContextUtil;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for deal lifecycle management.
 */
@RestController
@RequestMapping("/api/v1/deals")
@RequiredArgsConstructor
class DealController {

    private final DealService dealService;
    private final DealAuthorizationPort dealAuthorizationPort;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DealDto create(@RequestBody @Valid CreateDealRequest request) {
        var userId = SecurityContextUtil.currentUserId().value();
        var command = new CreateDealCommand(
                request.channelId(),
                request.amountNano(),
                request.pricingRuleId(),
                request.creativeBrief());
        return dealService.create(command, userId);
    }

    @GetMapping("/{id}")
    DealDetailDto getDetail(@PathVariable UUID id) {
        return dealService.getDetail(DealId.of(id));
    }

    @GetMapping
    CursorPage<DealDto> list(
            @RequestParam(required = false) DealStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        var userId = SecurityContextUtil.currentUserId().value();
        var criteria = new DealListCriteria(status, cursor, limit);
        return dealService.listForUser(criteria, userId);
    }

    @PostMapping("/{id}/transition")
    DealTransitionResponse transition(
            @PathVariable UUID id,
            @RequestBody @Valid DealTransitionRequest request) {
        var dealId = DealId.of(id);
        var actorType = resolveActorType(dealId);
        Long actorId = actorType == ActorType.SYSTEM
                ? null
                : SecurityContextUtil.currentUserId().value();

        var command = new DealTransitionCommand(
                dealId, request.targetStatus(),
                actorId, actorType, request.reason());

        var result = dealService.transition(command);
        return toResponse(result);
    }

    private ActorType resolveActorType(DealId dealId) {
        if (SecurityContextUtil.isOperator()) {
            return ActorType.PLATFORM_OPERATOR;
        }
        if (dealAuthorizationPort.isAdvertiser(dealId)) {
            return ActorType.ADVERTISER;
        }
        if (dealAuthorizationPort.isOwner(dealId)) {
            return ActorType.CHANNEL_OWNER;
        }
        return ActorType.ADVERTISER;
    }

    private DealTransitionResponse toResponse(DealTransitionResult result) {
        return switch (result) {
            case null -> throw new IllegalArgumentException(
                    "Transition result must not be null");
            case DealTransitionResult.Success s ->
                    new DealTransitionResponse("SUCCESS", s.newStatus(), null);
            case DealTransitionResult.AlreadyInTargetState a ->
                    new DealTransitionResponse(
                            "ALREADY_IN_TARGET_STATE", null, a.currentStatus());
        };
    }
}
