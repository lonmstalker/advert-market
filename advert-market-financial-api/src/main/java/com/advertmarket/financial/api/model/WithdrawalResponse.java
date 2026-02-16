package com.advertmarket.financial.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Response after initiating a withdrawal.
 *
 * @param withdrawalId unique identifier for the withdrawal
 * @param status       initial status (PENDING)
 * @param amountNano   amount in nanoTON
 * @param toAddress    destination TON address
 */
@Schema(description = "Withdrawal response")
public record WithdrawalResponse(
        @Schema(description = "Withdrawal ID")
        @NonNull String withdrawalId,
        @Schema(description = "Status", example = "PENDING")
        @NonNull String status,
        @Schema(description = "Amount in nanoTON")
        long amountNano,
        @Schema(description = "Destination TON address")
        @NonNull String toAddress
) {
}
