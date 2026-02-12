package com.advertmarket.deal.api.port;

import com.advertmarket.shared.model.DealId;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for deal-level authorization checks (ABAC).
 *
 * <p>Implementations verify whether the current authenticated user
 * has the required relationship with a deal.
 */
public interface DealAuthorizationPort {

    /** Returns {@code true} if the current user is a participant in the deal. */
    boolean isParticipant(@NonNull DealId dealId);

    /** Returns {@code true} if the current user is the advertiser of the deal. */
    boolean isAdvertiser(@NonNull DealId dealId);

    /** Returns {@code true} if the current user is the channel owner of the deal. */
    boolean isOwner(@NonNull DealId dealId);

    /** Returns the channel ID associated with the deal. */
    long channelId(@NonNull DealId dealId);
}
