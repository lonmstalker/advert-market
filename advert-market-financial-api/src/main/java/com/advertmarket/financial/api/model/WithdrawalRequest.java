package com.advertmarket.financial.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Request to withdraw funds from the user's available balance.
 *
 * @param amountNano withdrawal amount in nanoTON (must be positive)
 */
@Schema(description = "Wallet withdrawal request")
public record WithdrawalRequest(
        @Schema(description = "Amount in nanoTON", example = "1000000000")
        @Positive
        long amountNano
) {
}
