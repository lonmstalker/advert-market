package com.advertmarket.financial.api.port;

import com.advertmarket.financial.api.model.DepositAddressInfo;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.UserId;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for escrow lifecycle operations.
 *
 * <p>Manages deposit addresses, ledger entries for escrow funding,
 * release (with commission split), and refund.
 */
public interface EscrowPort {

    /**
     * Generates a deposit address and creates a pending TON transaction.
     *
     * @param dealId      the deal identifier
     * @param amountNano  expected deposit amount in nanoTON
     * @return deposit address info (address + subwallet ID)
     */
    @NonNull DepositAddressInfo generateDepositAddress(
            @NonNull DealId dealId, long amountNano);

    /**
     * Confirms a deposit via double-entry ledger:
     * DEBIT EXTERNAL_TON / CREDIT ESCROW:{dealId}.
     *
     * @param dealId        the deal identifier
     * @param txHash        blockchain transaction hash
     * @param amountNano    confirmed deposit amount
     * @param confirmations number of block confirmations
     * @param fromAddress   sender's TON address
     */
    void confirmDeposit(@NonNull DealId dealId,
                        @NonNull String txHash,
                        long amountNano,
                        int confirmations,
                        @NonNull String fromAddress);

    /**
     * Releases escrow with commission split:
     * DEBIT ESCROW → CREDIT COMMISSION + CREDIT OWNER_PENDING.
     *
     * @param dealId          the deal identifier
     * @param ownerId         the channel owner's user identifier
     * @param dealAmountNano  total deal amount in nanoTON
     * @param commissionRateBp commission rate in basis points
     */
    void releaseEscrow(@NonNull DealId dealId,
                       @NonNull UserId ownerId,
                       long dealAmountNano,
                       int commissionRateBp);

    /**
     * Refunds escrow to external TON:
     * DEBIT ESCROW / CREDIT EXTERNAL_TON.
     *
     * @param dealId     the deal identifier
     * @param amountNano amount to refund in nanoTON
     */
    void refundEscrow(@NonNull DealId dealId, long amountNano);
}
