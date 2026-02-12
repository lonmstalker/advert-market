package com.advertmarket.financial.api.event;

import com.advertmarket.shared.event.DomainEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Command to start watching for a TON deposit.
 *
 * @param depositAddress TON deposit address to watch
 * @param expectedAmountNano expected deposit amount in nanoTON
 * @param advertiserId advertiser's user ID
 */
public record WatchDepositCommand(
        @NonNull String depositAddress,
        long expectedAmountNano,
        long advertiserId) implements DomainEvent {
}
