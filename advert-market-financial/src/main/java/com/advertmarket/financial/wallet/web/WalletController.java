package com.advertmarket.financial.wallet.web;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.WalletSummary;
import com.advertmarket.financial.api.model.WithdrawalRequest;
import com.advertmarket.financial.api.model.WithdrawalResponse;
import com.advertmarket.financial.api.port.WalletPort;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.security.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for user wallet balances, transaction history, and withdrawals.
 */
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
class WalletController {

    private final WalletPort walletPort;

    @GetMapping("/summary")
    WalletSummary getSummary() {
        var userId = SecurityContextUtil.currentUserId();
        return walletPort.getSummary(userId);
    }

    @GetMapping("/transactions")
    CursorPage<LedgerEntry> getTransactions(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        var userId = SecurityContextUtil.currentUserId();
        return walletPort.getTransactions(userId, cursor, limit);
    }

    @PostMapping("/withdraw")
    WithdrawalResponse withdraw(
            @Valid @RequestBody WithdrawalRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        var userId = SecurityContextUtil.currentUserId();
        return walletPort.withdraw(
                userId, request.amountNano(), idempotencyKey);
    }
}
