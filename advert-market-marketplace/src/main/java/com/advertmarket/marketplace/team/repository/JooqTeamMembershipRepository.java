package com.advertmarket.marketplace.team.repository;

import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.Users.USERS;

import com.advertmarket.marketplace.api.dto.TeamMemberDto;
import com.advertmarket.marketplace.api.model.ChannelMembershipRole;
import com.advertmarket.marketplace.api.model.ChannelRight;
import com.advertmarket.marketplace.api.port.TeamMembershipRepository;
import com.advertmarket.marketplace.team.mapper.ChannelRightsJsonConverter;
import com.advertmarket.marketplace.team.mapper.TeamMemberDtoMapper;
import com.advertmarket.marketplace.team.mapper.TeamMemberRow;
import com.advertmarket.shared.json.JsonFacade;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Implements {@link TeamMembershipRepository} using jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqTeamMembershipRepository implements TeamMembershipRepository {

    private final DSLContext dsl;
    private final JsonFacade jsonFacade;
    private final TeamMemberDtoMapper mapper;

    @Override
    @NonNull
    public List<TeamMemberDto> findByChannelId(long channelId) {
        var rows = dsl.select(
                        CHANNEL_MEMBERSHIPS.USER_ID.as("userId"),
                        USERS.USERNAME.as("username"),
                        USERS.FIRST_NAME.as("firstName"),
                        CHANNEL_MEMBERSHIPS.ROLE.as("role"),
                        CHANNEL_MEMBERSHIPS.RIGHTS.as("rights"),
                        CHANNEL_MEMBERSHIPS.INVITED_BY.as("invitedBy"),
                        CHANNEL_MEMBERSHIPS.CREATED_AT.as("createdAt"))
                .from(CHANNEL_MEMBERSHIPS)
                .join(USERS).on(USERS.ID.eq(CHANNEL_MEMBERSHIPS.USER_ID))
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .orderBy(CHANNEL_MEMBERSHIPS.ROLE.asc(),
                        CHANNEL_MEMBERSHIPS.CREATED_AT.asc())
                .fetchInto(TeamMemberRow.class);
        return rows.stream()
                .map(r -> mapper.toDto(r, jsonFacade))
                .toList();
    }

    @Override
    @NonNull
    public Optional<ChannelMembershipRole> findRole(long channelId,
                                                     long userId) {
        return dsl.select(CHANNEL_MEMBERSHIPS.ROLE)
                .from(CHANNEL_MEMBERSHIPS)
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_MEMBERSHIPS.USER_ID.eq(userId))
                .fetchOptional(CHANNEL_MEMBERSHIPS.ROLE)
                .map(mapper::toRole);
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
                        ChannelRightsJsonConverter.rightsToJson(
                                rights, jsonFacade))
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

        return mapper.toDto(new TeamMemberRow(
                record.getUserId(),
                username,
                firstName,
                record.getRole(),
                record.getRights(),
                record.getInvitedBy(),
                record.getCreatedAt()), jsonFacade);
    }

    @Override
    @NonNull
    public Optional<TeamMemberDto> updateRights(long channelId, long userId,
                                                 @NonNull Set<ChannelRight> rights) {
        int updated = dsl.update(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.RIGHTS,
                        ChannelRightsJsonConverter.rightsToJson(
                                rights, jsonFacade))
                .set(CHANNEL_MEMBERSHIPS.VERSION,
                        CHANNEL_MEMBERSHIPS.VERSION.plus(1))
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_MEMBERSHIPS.USER_ID.eq(userId))
                .execute();

        if (updated == 0) {
            return Optional.empty();
        }

        return dsl.select(
                        CHANNEL_MEMBERSHIPS.USER_ID.as("userId"),
                        USERS.USERNAME.as("username"),
                        USERS.FIRST_NAME.as("firstName"),
                        CHANNEL_MEMBERSHIPS.ROLE.as("role"),
                        CHANNEL_MEMBERSHIPS.RIGHTS.as("rights"),
                        CHANNEL_MEMBERSHIPS.INVITED_BY.as("invitedBy"),
                        CHANNEL_MEMBERSHIPS.CREATED_AT.as("createdAt"))
                .from(CHANNEL_MEMBERSHIPS)
                .join(USERS).on(USERS.ID.eq(CHANNEL_MEMBERSHIPS.USER_ID))
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_MEMBERSHIPS.USER_ID.eq(userId))
                .fetchOptionalInto(TeamMemberRow.class)
                .map(r -> mapper.toDto(r, jsonFacade));
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
}
