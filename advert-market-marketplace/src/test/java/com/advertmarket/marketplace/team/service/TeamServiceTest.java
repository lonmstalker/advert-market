package com.advertmarket.marketplace.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.security.PrincipalAuthentication;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("TeamService — channel team management")
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    private static final long CHANNEL_ID = -1001234567890L;
    private static final long OWNER_ID = 1L;
    private static final long MANAGER_ID = 2L;
    private static final long TARGET_USER_ID = 3L;

    @Mock
    private TeamMembershipRepository teamRepository;
    @Mock
    private ChannelAuthorizationPort authorizationPort;
    @Mock
    private ChannelAutoSyncPort channelAutoSyncPort;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private TeamProperties teamProperties;

    @InjectMocks
    private TeamService teamService;

    @BeforeEach
    void setUpSecurityContext() {
        setCurrentUser(OWNER_ID);
        lenient().when(teamProperties.maxManagers()).thenReturn(10);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("listMembers")
    class ListMembers {

        @Test
        @DisplayName("Should list members when user has manage_team right")
        void shouldListMembers() {
            when(authorizationPort.hasRight(CHANNEL_ID, "manage_team"))
                    .thenReturn(true);
            when(teamRepository.findByChannelId(CHANNEL_ID))
                    .thenReturn(List.of(ownerMember(), managerMember()));

            var result = teamService.listMembers(CHANNEL_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should throw when user lacks manage_team right")
        void shouldThrowWhenNoRight() {
            when(authorizationPort.hasRight(CHANNEL_ID, "manage_team"))
                    .thenReturn(false);

            assertThatThrownBy(
                    () -> teamService.listMembers(CHANNEL_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.CHANNEL_NOT_OWNED);
        }
    }

    @Nested
    @DisplayName("invite")
    class Invite {

        @Test
        @DisplayName("Should invite user when owner and valid request")
        void shouldInviteUser() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(channelRepository.existsByTelegramId(CHANNEL_ID))
                    .thenReturn(true);
            when(teamRepository.userExists(TARGET_USER_ID))
                    .thenReturn(true);
            when(teamRepository.countManagers(CHANNEL_ID)).thenReturn(0);
            when(teamRepository.insert(eq(CHANNEL_ID), eq(TARGET_USER_ID),
                    any(), eq(OWNER_ID)))
                    .thenReturn(managerMember(TARGET_USER_ID));

            var request = new TeamInviteRequest(TARGET_USER_ID,
                    Set.of(ChannelRight.MODERATE));
            var result = teamService.invite(CHANNEL_ID, request);

            assertThat(result.userId()).isEqualTo(TARGET_USER_ID);
            assertThat(result.role()).isEqualTo(
                    ChannelMembershipRole.MANAGER);
        }

        @Test
        @DisplayName("Should throw CHANNEL_NOT_OWNED when not owner")
        void shouldThrowWhenNotOwner() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(false);

            var request = new TeamInviteRequest(TARGET_USER_ID,
                    Set.of(ChannelRight.MODERATE));
            assertThatThrownBy(
                    () -> teamService.invite(CHANNEL_ID, request))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.CHANNEL_NOT_OWNED);
        }

        @Test
        @DisplayName("Should throw CHANNEL_NOT_FOUND when channel missing")
        void shouldThrowWhenChannelMissing() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(channelRepository.existsByTelegramId(CHANNEL_ID))
                    .thenReturn(false);

            var request = new TeamInviteRequest(TARGET_USER_ID,
                    Set.of(ChannelRight.MODERATE));
            assertThatThrownBy(
                    () -> teamService.invite(CHANNEL_ID, request))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.CHANNEL_NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw VALIDATION_FAILED on self-invite")
        void shouldThrowOnSelfInvite() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(channelRepository.existsByTelegramId(CHANNEL_ID))
                    .thenReturn(true);

            var request = new TeamInviteRequest(OWNER_ID,
                    Set.of(ChannelRight.MODERATE));
            assertThatThrownBy(
                    () -> teamService.invite(CHANNEL_ID, request))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when target user missing")
        void shouldThrowWhenUserMissing() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(channelRepository.existsByTelegramId(CHANNEL_ID))
                    .thenReturn(true);
            when(teamRepository.userExists(TARGET_USER_ID))
                    .thenReturn(false);

            var request = new TeamInviteRequest(TARGET_USER_ID,
                    Set.of(ChannelRight.MODERATE));
            assertThatThrownBy(
                    () -> teamService.invite(CHANNEL_ID, request))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw TEAM_LIMIT_EXCEEDED when 10 managers")
        void shouldThrowWhenLimitExceeded() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(channelRepository.existsByTelegramId(CHANNEL_ID))
                    .thenReturn(true);
            when(teamRepository.userExists(TARGET_USER_ID))
                    .thenReturn(true);
            when(teamRepository.countManagers(CHANNEL_ID)).thenReturn(10);

            var request = new TeamInviteRequest(TARGET_USER_ID,
                    Set.of(ChannelRight.MODERATE));
            assertThatThrownBy(
                    () -> teamService.invite(CHANNEL_ID, request))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.TEAM_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("Should throw TEAM_MEMBER_ALREADY_EXISTS on duplicate")
        void shouldThrowOnDuplicate() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(channelRepository.existsByTelegramId(CHANNEL_ID))
                    .thenReturn(true);
            when(teamRepository.userExists(TARGET_USER_ID))
                    .thenReturn(true);
            when(teamRepository.countManagers(CHANNEL_ID)).thenReturn(0);
            when(teamRepository.insert(eq(CHANNEL_ID), eq(TARGET_USER_ID),
                    any(), eq(OWNER_ID)))
                    .thenThrow(new DuplicateKeyException("unique"));

            var request = new TeamInviteRequest(TARGET_USER_ID,
                    Set.of(ChannelRight.MODERATE));
            assertThatThrownBy(
                    () -> teamService.invite(CHANNEL_ID, request))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.TEAM_MEMBER_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("Should allow invite with empty rights set")
        void shouldAllowEmptyRights() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(channelRepository.existsByTelegramId(CHANNEL_ID))
                    .thenReturn(true);
            when(teamRepository.userExists(TARGET_USER_ID))
                    .thenReturn(true);
            when(teamRepository.countManagers(CHANNEL_ID)).thenReturn(0);
            when(teamRepository.insert(eq(CHANNEL_ID), eq(TARGET_USER_ID),
                    any(), eq(OWNER_ID)))
                    .thenReturn(managerMember(TARGET_USER_ID, Set.of()));

            var request = new TeamInviteRequest(TARGET_USER_ID, Set.of());
            var result = teamService.invite(CHANNEL_ID, request);

            assertThat(result.rights()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateRights")
    class UpdateRights {

        @Test
        @DisplayName("Should update rights when owner")
        void shouldUpdateRights() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(teamRepository.findRole(CHANNEL_ID, MANAGER_ID))
                    .thenReturn(Optional.of(
                            ChannelMembershipRole.MANAGER));
            when(teamRepository.updateRights(eq(CHANNEL_ID),
                    eq(MANAGER_ID), any()))
                    .thenReturn(Optional.of(managerMember()));

            var request = new TeamUpdateRightsRequest(
                    Set.of(ChannelRight.PUBLISH));
            var result = teamService.updateRights(
                    CHANNEL_ID, MANAGER_ID, request);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should throw TEAM_OWNER_PROTECTED when updating owner")
        void shouldThrowWhenUpdatingOwner() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(teamRepository.findRole(CHANNEL_ID, OWNER_ID))
                    .thenReturn(Optional.of(
                            ChannelMembershipRole.OWNER));

            setCurrentUser(OWNER_ID);
            var request = new TeamUpdateRightsRequest(
                    Set.of(ChannelRight.PUBLISH));
            assertThatThrownBy(() -> teamService.updateRights(
                    CHANNEL_ID, OWNER_ID, request))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.TEAM_OWNER_PROTECTED);
        }

        @Test
        @DisplayName("Should throw TEAM_MEMBER_NOT_FOUND when missing")
        void shouldThrowWhenMemberMissing() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(teamRepository.findRole(CHANNEL_ID, TARGET_USER_ID))
                    .thenReturn(Optional.empty());
            when(teamRepository.updateRights(eq(CHANNEL_ID),
                    eq(TARGET_USER_ID), any()))
                    .thenReturn(Optional.empty());

            var request = new TeamUpdateRightsRequest(
                    Set.of(ChannelRight.PUBLISH));
            assertThatThrownBy(() -> teamService.updateRights(
                    CHANNEL_ID, TARGET_USER_ID, request))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.TEAM_MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("removeMember")
    class RemoveMember {

        @Test
        @DisplayName("Should remove manager when owner")
        void shouldRemoveManager() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(teamRepository.findRole(CHANNEL_ID, MANAGER_ID))
                    .thenReturn(Optional.of(
                            ChannelMembershipRole.MANAGER));
            when(teamRepository.delete(CHANNEL_ID, MANAGER_ID))
                    .thenReturn(true);

            teamService.removeMember(CHANNEL_ID, MANAGER_ID);

            verify(teamRepository).delete(CHANNEL_ID, MANAGER_ID);
        }

        @Test
        @DisplayName("Should allow self-removal by manager")
        void shouldAllowSelfRemoval() {
            setCurrentUser(MANAGER_ID);
            when(teamRepository.findRole(CHANNEL_ID, MANAGER_ID))
                    .thenReturn(Optional.of(
                            ChannelMembershipRole.MANAGER));
            when(teamRepository.delete(CHANNEL_ID, MANAGER_ID))
                    .thenReturn(true);

            teamService.removeMember(CHANNEL_ID, MANAGER_ID);

            verify(teamRepository).delete(CHANNEL_ID, MANAGER_ID);
            verify(authorizationPort, never()).isOwner(anyLong());
        }

        @Test
        @DisplayName("Should throw TEAM_OWNER_PROTECTED on owner self-removal")
        void shouldThrowOnOwnerSelfRemoval() {
            setCurrentUser(OWNER_ID);
            when(teamRepository.findRole(CHANNEL_ID, OWNER_ID))
                    .thenReturn(Optional.of(
                            ChannelMembershipRole.OWNER));

            assertThatThrownBy(
                    () -> teamService.removeMember(CHANNEL_ID, OWNER_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.TEAM_OWNER_PROTECTED);
        }

        @Test
        @DisplayName("Should throw TEAM_OWNER_PROTECTED when removing owner")
        void shouldThrowWhenRemovingOwnerByAnotherUser() {
            setCurrentUser(MANAGER_ID);
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(teamRepository.findRole(CHANNEL_ID, OWNER_ID))
                    .thenReturn(Optional.of(
                            ChannelMembershipRole.OWNER));

            assertThatThrownBy(
                    () -> teamService.removeMember(CHANNEL_ID, OWNER_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.TEAM_OWNER_PROTECTED);
        }

        @Test
        @DisplayName("Should throw TEAM_MEMBER_NOT_FOUND when missing")
        void shouldThrowWhenMemberMissing() {
            when(authorizationPort.isOwner(CHANNEL_ID)).thenReturn(true);
            when(teamRepository.findRole(CHANNEL_ID, TARGET_USER_ID))
                    .thenReturn(Optional.empty());
            when(teamRepository.delete(CHANNEL_ID, TARGET_USER_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> teamService.removeMember(
                    CHANNEL_ID, TARGET_USER_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCodes.TEAM_MEMBER_NOT_FOUND);
        }
    }

    // --- helpers ---

    private static void setCurrentUser(long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new PrincipalAuthentication() {
                    @Override
                    public UserId getUserId() {
                        return new UserId(userId);
                    }

                    @Override
                    public String getJti() {
                        return "test-jti";
                    }

                    @Override
                    public boolean isOperator() {
                        return false;
                    }

                    @Override
                    public long getTokenExpSeconds() {
                        return 0;
                    }

                    @Override
                    public Collection<? extends GrantedAuthority> getAuthorities() {
                        return List.of();
                    }

                    @Override
                    public Object getCredentials() {
                        return null;
                    }

                    @Override
                    public Object getDetails() {
                        return null;
                    }

                    @Override
                    public Object getPrincipal() {
                        return new UserId(userId);
                    }

                    @Override
                    public boolean isAuthenticated() {
                        return true;
                    }

                    @Override
                    public void setAuthenticated(boolean isAuthenticated) {
                    }

                    @Override
                    public String getName() {
                        return String.valueOf(userId);
                    }
                });
    }

    private static TeamMemberDto ownerMember() {
        return new TeamMemberDto(
                OWNER_ID, "owner", "Owner",
                ChannelMembershipRole.OWNER, Set.of(),
                null, OffsetDateTime.now());
    }

    private static TeamMemberDto managerMember() {
        return managerMember(MANAGER_ID);
    }

    private static TeamMemberDto managerMember(long userId) {
        return managerMember(userId,
                Set.of(ChannelRight.MODERATE));
    }

    private static TeamMemberDto managerMember(long userId,
                                                Set<ChannelRight> rights) {
        return new TeamMemberDto(
                userId, "user" + userId, "User" + userId,
                ChannelMembershipRole.MANAGER, rights,
                OWNER_ID, OffsetDateTime.now());
    }
}
