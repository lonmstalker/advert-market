package com.advertmarket.marketplace.team.repository;

import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.Users.USERS;

import com.advertmarket.marketplace.api.dto.TeamMemberDto;
import com.advertmarket.marketplace.api.model.ChannelMembershipRole;
import com.advertmarket.marketplace.api.model.ChannelRight;
import com.advertmarket.marketplace.api.port.TeamMembershipRepository;
import com.advertmarket.shared.json.JsonFacade;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.springframework.stereotype.Repository;

/**
 * Implements {@link TeamMembershipRepository} using jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqTeamMembershipRepository implements TeamMembershipRepository {

    private final DSLContext dsl;
    private final JsonFacade jsonFacade;

    @Override
    @NonNull
    public List<TeamMemberDto> findByChannelId(long channelId) {
        return dsl.select(
                        CHANNEL_MEMBERSHIPS.USER_ID,
                        USERS.USERNAME,
                        USERS.FIRST_NAME,
                        CHANNEL_MEMBERSHIPS.ROLE,
                        CHANNEL_MEMBERSHIPS.RIGHTS,
                        CHANNEL_MEMBERSHIPS.INVITED_BY,
                        CHANNEL_MEMBERSHIPS.CREATED_AT)
                .from(CHANNEL_MEMBERSHIPS)
                .join(USERS).on(USERS.ID.eq(CHANNEL_MEMBERSHIPS.USER_ID))
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .orderBy(CHANNEL_MEMBERSHIPS.ROLE.asc(),
                        CHANNEL_MEMBERSHIPS.CREATED_AT.asc())
                .fetch(r -> new TeamMemberDto(
                        r.get(CHANNEL_MEMBERSHIPS.USER_ID),
                        r.get(USERS.USERNAME),
                        r.get(USERS.FIRST_NAME),
                        ChannelMembershipRole.valueOf(
                                r.get(CHANNEL_MEMBERSHIPS.ROLE)),
                        parseRights(r.get(CHANNEL_MEMBERSHIPS.RIGHTS)),
                        r.get(CHANNEL_MEMBERSHIPS.INVITED_BY),
                        r.get(CHANNEL_MEMBERSHIPS.CREATED_AT)));
    }

    @Override
    @NonNull
    public Optional<ChannelMembershipRole> findRole(long channelId,
                                                     long userId) {
        return dsl.select(CHANNEL_MEMBERSHIPS.ROLE)
                .from(CHANNEL_MEMBERSHIPS)
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_MEMBERSHIPS.USER_ID.eq(userId))
                .fetchOptional(r -> ChannelMembershipRole.valueOf(
                        r.get(CHANNEL_MEMBERSHIPS.ROLE)));
    }

    @Override
    @NonNull
    public TeamMemberDto insert(long channelId, long userId,
                                 @NonNull Set<ChannelRight> rights,
                                 long invitedBy) {
        var record = dsl.insertInto(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.CHANNEL_ID, channelId)
                .set(CHANNEL_MEMBERSHIPS.USER_ID, userId)
                .set(CHANNEL_MEMBERSHIPS.ROLE,
                        ChannelMembershipRole.MANAGER.name())
                .set(CHANNEL_MEMBERSHIPS.RIGHTS,
                        rightsToJson(rights))
                .set(CHANNEL_MEMBERSHIPS.INVITED_BY, invitedBy)
                .returning()
                .fetchSingle();

        var userInfo = dsl.select(USERS.USERNAME, USERS.FIRST_NAME)
                .from(USERS)
                .where(USERS.ID.eq(userId))
                .fetchOne();
        String username = userInfo != null ? userInfo.get(USERS.USERNAME) : null;
        String firstName = userInfo != null
                ? userInfo.get(USERS.FIRST_NAME)
                : "";

        return new TeamMemberDto(
                record.getUserId(),
                username,
                firstName,
                ChannelMembershipRole.MANAGER,
                Set.copyOf(rights),
                record.getInvitedBy(),
                record.getCreatedAt());
    }

    @Override
    @NonNull
    public Optional<TeamMemberDto> updateRights(long channelId, long userId,
                                                 @NonNull Set<ChannelRight> rights) {
        int updated = dsl.update(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.RIGHTS, rightsToJson(rights))
                .set(CHANNEL_MEMBERSHIPS.VERSION,
                        CHANNEL_MEMBERSHIPS.VERSION.plus(1))
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_MEMBERSHIPS.USER_ID.eq(userId))
                .execute();

        if (updated == 0) {
            return Optional.empty();
        }

        return dsl.select(
                        CHANNEL_MEMBERSHIPS.USER_ID,
                        USERS.USERNAME,
                        USERS.FIRST_NAME,
                        CHANNEL_MEMBERSHIPS.ROLE,
                        CHANNEL_MEMBERSHIPS.RIGHTS,
                        CHANNEL_MEMBERSHIPS.INVITED_BY,
                        CHANNEL_MEMBERSHIPS.CREATED_AT)
                .from(CHANNEL_MEMBERSHIPS)
                .join(USERS).on(USERS.ID.eq(CHANNEL_MEMBERSHIPS.USER_ID))
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_MEMBERSHIPS.USER_ID.eq(userId))
                .fetchOptional(r -> new TeamMemberDto(
                        r.get(CHANNEL_MEMBERSHIPS.USER_ID),
                        r.get(USERS.USERNAME),
                        r.get(USERS.FIRST_NAME),
                        ChannelMembershipRole.valueOf(
                                r.get(CHANNEL_MEMBERSHIPS.ROLE)),
                        parseRights(r.get(CHANNEL_MEMBERSHIPS.RIGHTS)),
                        r.get(CHANNEL_MEMBERSHIPS.INVITED_BY),
                        r.get(CHANNEL_MEMBERSHIPS.CREATED_AT)));
    }

    @Override
    public boolean delete(long channelId, long userId) {
        return dsl.deleteFrom(CHANNEL_MEMBERSHIPS)
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_MEMBERSHIPS.USER_ID.eq(userId))
                .execute() > 0;
    }

    @Override
    public int countManagers(long channelId) {
        return dsl.selectCount()
                .from(CHANNEL_MEMBERSHIPS)
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_MEMBERSHIPS.ROLE.eq(
                        ChannelMembershipRole.MANAGER.name()))
                .fetchSingle(0, int.class);
    }

    @Override
    public boolean userExists(long userId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(USERS)
                        .where(USERS.ID.eq(userId)));
    }

    @SuppressWarnings("unchecked")
    private Set<ChannelRight> parseRights(JSON json) {
        if (json == null || json.data() == null
                || json.data().isBlank()
                || "{}".equals(json.data())) {
            return Set.of();
        }
        Map<String, Object> map = jsonFacade.fromJson(
                json.data(), Map.class);
        var result = EnumSet.noneOf(ChannelRight.class);
        for (var entry : map.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                result.add(ChannelRight.valueOf(
                        entry.getKey().toUpperCase()));
            }
        }
        return result;
    }

    private JSON rightsToJson(@NonNull Set<ChannelRight> rights) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (ChannelRight right : rights) {
            map.put(right.name().toLowerCase(), true);
        }
        return JSON.json(jsonFacade.toJson(map));
    }
}
