package com.advertmarket.marketplace.api.model;

/**
 * Granular rights for channel team members (MANAGER role).
 *
 * <p>OWNER has all rights implicitly. MANAGER rights are stored
 * in the {@code rights} JSONB column of {@code channel_memberships}.
 */
public enum ChannelRight {
    MODERATE,
    PUBLISH,
    MANAGE_LISTINGS,
    MANAGE_TEAM,
    VIEW_STATS
}
