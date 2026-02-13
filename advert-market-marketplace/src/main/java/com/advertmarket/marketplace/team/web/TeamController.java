package com.advertmarket.marketplace.team.web;

import com.advertmarket.marketplace.api.dto.TeamInviteRequest;
import com.advertmarket.marketplace.api.dto.TeamMemberDto;
import com.advertmarket.marketplace.api.dto.TeamUpdateRightsRequest;
import com.advertmarket.marketplace.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Channel team management endpoints.
 */
@RestController
@RequestMapping("/api/v1/channels/{channelId}/team")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Channel Team", description = "Channel team management")
public class TeamController {

    private final TeamService teamService;

    /**
     * Lists all team members for a channel.
     */
    @GetMapping
    @Operation(summary = "List team members",
            description = "Lists all team members (requires manage_team right)")
    @ApiResponse(responseCode = "200", description = "List of team members")
    @ApiResponse(responseCode = "403",
            description = "Insufficient rights")
    public List<TeamMemberDto> list(
            @PathVariable("channelId") long channelId) {
        return teamService.listMembers(channelId);
    }

    /**
     * Invites a user to the channel team. Owner only.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite team member",
            description = "Invites a user as MANAGER (owner only)")
    @ApiResponse(responseCode = "201", description = "Member invited")
    @ApiResponse(responseCode = "403",
            description = "Not the owner of the channel")
    @ApiResponse(responseCode = "409",
            description = "User is already a team member")
    @ApiResponse(responseCode = "422",
            description = "Manager limit exceeded")
    public TeamMemberDto invite(
            @PathVariable("channelId") long channelId,
            @Valid @RequestBody TeamInviteRequest request) {
        return teamService.invite(channelId, request);
    }

    /**
     * Updates rights for a team member. Owner only.
     */
    @PutMapping("/{userId}")
    @Operation(summary = "Update member rights",
            description = "Updates rights for a MANAGER (owner only)")
    @ApiResponse(responseCode = "200",
            description = "Rights updated")
    @ApiResponse(responseCode = "403",
            description = "Not the owner or target is OWNER")
    @ApiResponse(responseCode = "404",
            description = "Team member not found")
    public TeamMemberDto updateRights(
            @PathVariable("channelId") long channelId,
            @PathVariable("userId") long userId,
            @Valid @RequestBody TeamUpdateRightsRequest request) {
        return teamService.updateRights(channelId, userId, request);
    }

    /**
     * Removes a team member. Owner or self-removal.
     */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove team member",
            description = "Removes a MANAGER (owner or self-removal)")
    @ApiResponse(responseCode = "204",
            description = "Member removed")
    @ApiResponse(responseCode = "403",
            description = "Not authorized or target is OWNER")
    @ApiResponse(responseCode = "404",
            description = "Team member not found")
    public void remove(
            @PathVariable("channelId") long channelId,
            @PathVariable("userId") long userId) {
        teamService.removeMember(channelId, userId);
    }
}
