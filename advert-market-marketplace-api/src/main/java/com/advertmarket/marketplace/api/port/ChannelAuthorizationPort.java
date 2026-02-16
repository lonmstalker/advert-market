package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.model.ChannelRight;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for channel-level authorization checks (ABAC).
 *
 * <p>Implementations verify whether the current authenticated user
 * has the required relationship with a channel.
 */
public interface ChannelAuthorizationPort {

    /** Returns {@code true} if the current user owns the channel. */
    boolean isOwner(long channelId);

    /** Returns {@code true} if the current user has the specified right on the channel. */
    boolean hasRight(long channelId, @NonNull ChannelRight right);
}
