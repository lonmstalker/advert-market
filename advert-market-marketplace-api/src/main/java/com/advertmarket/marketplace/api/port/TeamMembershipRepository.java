package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.TeamMemberDto;
import com.advertmarket.marketplace.api.model.ChannelMembershipRole;
import com.advertmarket.marketplace.api.model.ChannelRight;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Repository port for channel team membership operations.
 */
public interface TeamMembershipRepository {

    /** Lists all team members for a channel, ordered by role then created_at. */
    @NonNull
    List<TeamMemberDto> findByChannelId(long channelId);

    /** Finds the role of a specific membership. */
    @NonNull
    Optional<ChannelMembershipRole> findRole(long channelId, long userId);

    /** Inserts a new MANAGER membership and returns the DTO. */
    @NonNull
    TeamMemberDto insert(long channelId, long userId,
                         @NonNull Set<ChannelRight> rights, long invitedBy);

    /** Updates rights for an existing membership. Returns the updated DTO. */
    @NonNull
    Optional<TeamMemberDto> updateRights(long channelId, long userId,
                                         @NonNull Set<ChannelRight> rights);

    /** Deletes a membership. Returns true if a row was deleted. */
    boolean delete(long channelId, long userId);

    /** Counts MANAGER members for a channel. */
    int countManagers(long channelId);

    /** Checks if a user exists in the users table. */
    boolean userExists(long userId);
}
