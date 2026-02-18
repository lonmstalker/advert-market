package com.advertmarket.financial.ton.service;

import java.time.Instant;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Deal candidate for unclaimed payout reminder/escalation processing.
 */
public record UnclaimedPayoutCandidate(
        @NonNull UUID dealId,
        long ownerId,
        long amountNano,
        long commissionNano,
        int subwalletId,
        @NonNull Instant completedAt,
        boolean hasTonAddress
) {
}
