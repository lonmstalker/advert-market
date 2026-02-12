package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Command to execute a refund to the advertiser.
 *
 * @param advertiserId advertiser's user ID
 * @param amountNano refund amount in nanoTON
 * @param refundAddress TON address to send refund to
 * @param subwalletId TON sub-wallet ID for the transaction
 */
public record ExecuteRefundCommand(
        long advertiserId,
        long amountNano,
        @NonNull String refundAddress,
        int subwalletId) implements DomainEvent {
}
