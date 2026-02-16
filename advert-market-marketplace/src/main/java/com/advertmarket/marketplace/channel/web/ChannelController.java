package com.advertmarket.marketplace.channel.web;

import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelListItem;
import com.advertmarket.marketplace.api.dto.ChannelRegistrationRequest;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.channel.service.ChannelRegistrationService;
import com.advertmarket.marketplace.channel.service.ChannelService;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
 * Channel verification, registration, search, and management endpoints.
 */
@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
@Tag(name = "Channels", description = "Channel catalog and management")
public class ChannelController {

    private final ChannelRegistrationService registrationService;
    private final ChannelService channelService;
    private final ChannelSearchCriteriaConverter criteriaConverter;

    /**
     * Searches active channels in the catalog.
     */
    @GetMapping
    @Operation(summary = "Search channels",
            description = "Public channel catalog with filters and BM25 search")
    @ApiResponse(responseCode = "200", description = "Search results")
    public CursorPage<ChannelListItem> search(
            @ParameterObject ChannelSearchRequestParams params) {
        var criteria = criteriaConverter.fromRequestParams(params);
        return channelService.search(criteria);
    }

    /**
     * Counts active channels in the catalog with the same filters as search.
     */
    @GetMapping("/count")
    @Operation(summary = "Count channels",
            description = "Returns channel count for given search filters")
    @ApiResponse(responseCode = "200", description = "Channel count")
    public long count(
            @ParameterObject ChannelCountRequestParams params) {
        var criteria = criteriaConverter.fromRequestParams(params);
        return channelService.count(criteria);
    }

    /**
     * Returns channels owned by the current user.
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "My channels",
            description = "Returns channels owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "User's channels")
    public List<ChannelResponse> myChannels() {
        long userId = SecurityContextUtil.currentUserId().value();
        return channelService.findByMemberUserId(userId);
    }

    /**
     * Returns full channel detail with pricing rules.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get channel detail",
            description = "Full channel info including pricing rules")
    @ApiResponse(responseCode = "200", description = "Channel detail")
    @ApiResponse(responseCode = "404", description = "Channel not found")
    public ChannelDetailResponse getDetail(@PathVariable("id") long id) {
        return channelService.getDetail(id);
    }

    /**
     * Updates channel details. Owner only.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update channel",
            description = "Update channel details (owner only)")
    @ApiResponse(responseCode = "200", description = "Channel updated")
    @ApiResponse(responseCode = "403",
            description = "Not the owner of the channel")
    @ApiResponse(responseCode = "404", description = "Channel not found")
    public ChannelResponse update(
            @PathVariable("id") long id,
            @Valid @RequestBody ChannelUpdateRequest request) {
        return channelService.update(id, request);
    }

    /**
     * Deactivates a channel. Owner only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate channel",
            description = "Soft-delete a channel (owner only)")
    @ApiResponse(responseCode = "204",
            description = "Channel deactivated")
    @ApiResponse(responseCode = "403",
            description = "Not the owner of the channel")
    @ApiResponse(responseCode = "404", description = "Channel not found")
    public void deactivate(@PathVariable("id") long id) {
        channelService.deactivate(id);
    }

    /**
     * Verifies a channel: checks bot admin status and user ownership.
     */
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Verify channel for registration",
            description = "Checks that the bot is an admin "
                    + "and the user owns the channel"
    )
    @ApiResponse(responseCode = "200",
            description = "Verification result")
    @ApiResponse(responseCode = "403",
            description = "Bot or user lacks required permissions")
    @ApiResponse(responseCode = "404",
            description = "Channel not found")
    public ChannelVerifyResponse verify(
            @Valid @RequestBody ChannelVerifyRequest request) {
        long userId = SecurityContextUtil.currentUserId().value();
        return registrationService.verify(
                request.channelUsername(), userId);
    }

    /**
     * Registers a verified channel.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a channel",
            description = "Registers a previously verified channel"
    )
    @ApiResponse(responseCode = "201",
            description = "Channel registered")
    @ApiResponse(responseCode = "403",
            description = "User is not an admin of the channel")
    @ApiResponse(responseCode = "409",
            description = "Channel already registered")
    public ChannelResponse register(
            @Valid @RequestBody ChannelRegistrationRequest request) {
        long userId = SecurityContextUtil.currentUserId().value();
        return registrationService.register(request, userId);
    }
}
