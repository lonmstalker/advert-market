package com.advertmarket.deal.web;

import com.advertmarket.deal.api.dto.DealDetailDto;
import com.advertmarket.deal.api.dto.DealDto;
import com.advertmarket.deal.usecase.DealUseCase;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.pagination.CursorPage;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for deal lifecycle management.
 */
@RestController
@RequestMapping("/api/v1/deals")
@RequiredArgsConstructor
class DealController {

    private final DealUseCase dealUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DealDto create(@RequestBody @Valid CreateDealRequest request) {
        return dealUseCase.create(request);
    }

    @GetMapping("/{id}")
    DealDetailDto getDetail(@PathVariable("id") UUID id) {
        return dealUseCase.getDetail(id);
    }

    @GetMapping
    CursorPage<DealDto> list(@ParameterObject DealListRequestParams params) {
        return dealUseCase.list(
                params.status(),
                params.cursor(),
                params.limitOrDefault());
    }

    @PostMapping("/{id}/transition")
    DealTransitionResponse transition(
            @PathVariable("id") UUID id,
            @RequestBody @Valid DealTransitionRequest request) {
        return dealUseCase.transition(id, request);
    }
}
