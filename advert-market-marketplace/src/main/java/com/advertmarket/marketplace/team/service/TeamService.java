package com.advertmarket.marketplace.team.service;

import com.advertmarket.marketplace.api.dto.TeamInviteRequest;
import com.advertmarket.marketplace.api.dto.TeamMemberDto;
import com.advertmarket.marketplace.api.dto.TeamUpdateRightsRequest;
import com.advertmarket.marketplace.api.model.ChannelMembershipRole;
import com.advertmarket.marketplace.api.model.ChannelRight;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.api.port.TeamMembershipRepository;
import com.advertmarket.marketplace.team.config.TeamProperties;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.security.SecurityContextUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Channel team management operations.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(TeamProperties.class)
public class TeamService {

    private final TeamMembershipRepository teamRepository;
    private final ChannelAuthorizationPort authorizationPort;
    private final ChannelAutoSyncPort channelAutoSyncPort;
    private final ChannelRepository channelRepository;
    private final TeamProperties teamProperties;

    /**
     * Lists all team members. Requires manage_team right.
     *
     * @param channelId channel ID
     * @return list of team members
     */
    @NonNull
    public List<TeamMemberDto> listMembers(long channelId) {
        requireRight(channelId, ChannelRight.MANAGE_TEAM);
        return teamRepository.findByChannelId(channelId);
    }

    /**
     * Invites a user as MANAGER. Owner only.
     *
     * @param channelId channel ID
     * @param request   invite request
     * @return created team member
     */
    @NonNull
    @Transactional
    public TeamMemberDto invite(long channelId,
                                @NonNull TeamInviteRequest request) {
        requireOwner(channelId);
        channelAutoSyncPort.syncFromTelegram(channelId);
        requireChannelExists(channelId);

        long currentUserId = SecurityContextUtil.currentUserId().value();
        if (request.userId() == currentUserId) {
            throw new DomainException(
                    ErrorCodes.VALIDATION_FAILED,
                    "Cannot invite yourself");
        }

        if (!teamRepository.userExists(request.userId())) {
            throw new DomainException(
                    ErrorCodes.USER_NOT_FOUND,
                    "Target user not found: " + request.userId());
        }

        int maxManagers = teamProperties.maxManagers();
        if (teamRepository.countManagers(channelId) >= maxManagers) {
            throw new DomainException(
                    ErrorCodes.TEAM_LIMIT_EXCEEDED,
                    "Maximum of " + maxManagers
                            + " managers per channel");
        }

        try {
            return teamRepository.insert(
                    channelId, request.userId(),
                    request.rights(), currentUserId);
        } catch (DuplicateKeyException e) {
            throw new DomainException(
                    ErrorCodes.TEAM_MEMBER_ALREADY_EXISTS,
                    "User is already a team member: "
                            + request.userId());
        }
    }

    /**
     * Updates rights for a team member. Owner only.
     *
     * @param channelId channel ID
     * @param userId    target user ID
     * @param request   new rights
     * @return updated team member
     */
    @NonNull
    @Transactional
    public TeamMemberDto updateRights(long channelId, long userId,
                                       @NonNull TeamUpdateRightsRequest request) {
        requireOwner(channelId);
        channelAutoSyncPort.syncFromTelegram(channelId);
        requireNotOwnerMember(channelId, userId);

        return teamRepository.updateRights(
                        channelId, userId, request.rights())
                .orElseThrow(() -> new DomainException(
                        ErrorCodes.TEAM_MEMBER_NOT_FOUND,
                        "Team member not found: " + userId));
    }

    /**
     * Removes a team member. Owner can remove any MANAGER.
     * MANAGER can remove themselves (self-removal).
     *
     * @param channelId channel ID
     * @param userId    target user ID
     */
    @Transactional
    public void removeMember(long channelId, long userId) {
        long currentUserId = SecurityContextUtil.currentUserId().value();
        boolean isSelfRemoval = currentUserId == userId;

        if (isSelfRemoval) {
            requireNotOwnerMember(channelId, userId);
        } else {
            requireOwner(channelId);
            requireNotOwnerMember(channelId, userId);
        }

        boolean deleted = teamRepository.delete(channelId, userId);
        if (!deleted) {
            throw new DomainException(
                    ErrorCodes.TEAM_MEMBER_NOT_FOUND,
                    "Team member not found: " + userId);
        }
    }

    private void requireOwner(long channelId) {
        if (!authorizationPort.isOwner(channelId)) {
            throw new DomainException(
                    ErrorCodes.CHANNEL_NOT_OWNED,
                    "Not the owner of channel: " + channelId);
        }
    }

    private void requireRight(long channelId, ChannelRight right) {
        if (!authorizationPort.hasRight(channelId, right.name().toLowerCase())) {
            throw new DomainException(
                    ErrorCodes.CHANNEL_NOT_OWNED,
                    "Insufficient rights on channel: " + channelId);
        }
    }

    private void requireChannelExists(long channelId) {
        if (!channelRepository.existsByTelegramId(channelId)) {
            throw new DomainException(
                    ErrorCodes.CHANNEL_NOT_FOUND,
                    "Channel not found: " + channelId);
        }
    }

    private void requireNotOwnerMember(long channelId, long userId) {
        var role = teamRepository.findRole(channelId, userId);
        if (role.isPresent()
                && role.get() == ChannelMembershipRole.OWNER) {
            throw new DomainException(
                    ErrorCodes.TEAM_OWNER_PROTECTED,
                    "Cannot modify or remove channel owner");
        }
    }
}
