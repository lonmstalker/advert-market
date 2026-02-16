package com.advertmarket.financial.api.port;

import com.advertmarket.financial.api.model.DepositInfo;
import com.advertmarket.shared.model.DealId;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Deposit query and operator-review port.
 */
public interface DepositPort {

    /**
     * Returns the current deposit projection for a deal.
     *
     * @param dealId deal identifier
     * @return deposit projection if exists
     */
    @NonNull Optional<DepositInfo> getDepositInfo(@NonNull DealId dealId);

    /**
     * Approves a pending operator-review deposit and emits confirmation event.
     *
     * @param dealId deal identifier
     */
    void approveDeposit(@NonNull DealId dealId);

    /**
     * Rejects a pending operator-review deposit and emits failed event.
     *
     * @param dealId deal identifier
     */
    void rejectDeposit(@NonNull DealId dealId);

    /**
     * Resolves refund destination from inbound deposit context.
     *
     * @param dealId deal identifier
     * @return advertiser/source TON address if available
     */
    @NonNull Optional<String> findRefundAddress(@NonNull DealId dealId);
}
