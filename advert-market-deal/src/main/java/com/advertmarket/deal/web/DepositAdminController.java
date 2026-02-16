package com.advertmarket.deal.web;

import com.advertmarket.deal.usecase.DealUseCase;
import com.advertmarket.financial.api.model.DepositInfo;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator review endpoints for large/flagged deposits.
 */
@RestController
@RequestMapping("/api/v1/admin/deposits")
@RequiredArgsConstructor
class DepositAdminController {

    private final DealUseCase dealUseCase;

    @PostMapping("/{id}/approve")
    DepositInfo approve(@PathVariable("id") UUID id) {
        return dealUseCase.approveDeposit(id);
    }

    @PostMapping("/{id}/reject")
    DepositInfo reject(@PathVariable("id") UUID id) {
        return dealUseCase.rejectDeposit(id);
    }
}
