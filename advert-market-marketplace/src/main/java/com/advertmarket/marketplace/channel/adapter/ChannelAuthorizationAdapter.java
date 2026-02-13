package com.advertmarket.marketplace.channel.adapter;

import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;

import com.advertmarket.marketplace.api.model.ChannelMembershipRole;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.shared.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Implements {@link ChannelAuthorizationPort} using channel_memberships table.
 */
@Component
@RequiredArgsConstructor
public class ChannelAuthorizationAdapter implements ChannelAuthorizationPort {

    private final DSLContext dsl;

    @Override
    public boolean isOwner(long channelId) {
        long userId = SecurityContextUtil.currentUserId().value();
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CHANNEL_MEMBERSHIPS)
                        .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                        .and(CHANNEL_MEMBERSHIPS.USER_ID.eq(userId))
                        .and(CHANNEL_MEMBERSHIPS.ROLE.eq(
                                ChannelMembershipRole.OWNER.name())));
    }

    @Override
    public boolean hasRight(long channelId, @NonNull String right) {
        long userId = SecurityContextUtil.currentUserId().value();
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CHANNEL_MEMBERSHIPS)
                        .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                        .and(CHANNEL_MEMBERSHIPS.USER_ID.eq(userId))
                        .and(CHANNEL_MEMBERSHIPS.ROLE
                                .eq(ChannelMembershipRole.OWNER.name())
                                .or(CHANNEL_MEMBERSHIPS.ROLE
                                        .eq(ChannelMembershipRole.MANAGER.name())
                                        .and(DSL.field(
                                                "rights->>{0}",
                                                String.class,
                                                DSL.val(right))
                                                .eq("true")))));
    }
}
