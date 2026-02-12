package com.advertmarket.marketplace.web;

import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelListItem;
import com.advertmarket.marketplace.api.dto.ChannelRegistrationRequest;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelSearchCriteria;
import com.advertmarket.marketplace.api.dto.ChannelSort;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.service.ChannelRegistrationService;
import com.advertmarket.marketplace.service.ChannelService;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * Searches active channels in the catalog.
     */
    @GetMapping
    @Operation(summary = "Search channels",
            description = "Public channel catalog with filters and BM25 search")
    @ApiResponse(responseCode = "200", description = "Search results")
    public CursorPage<ChannelListItem> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minSubscribers,
            @RequestParam(required = false) Integer maxSubscribers,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Double minEngagement,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "SUBSCRIBERS_DESC") ChannelSort sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {

        var criteria = new ChannelSearchCriteria(
                category, minSubscribers, maxSubscribers,
                minPrice, maxPrice, minEngagement,
                language, query, sort, cursor, limit);
        return channelService.search(criteria);
    }

    /**
     * Returns full channel detail with pricing rules.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get channel detail",
            description = "Full channel info including pricing rules")
    @ApiResponse(responseCode = "200", description = "Channel detail")
    @ApiResponse(responseCode = "404", description = "Channel not found")
    public ChannelDetailResponse getDetail(@PathVariable long id) {
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
            @PathVariable long id,
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
    public void deactivate(@PathVariable long id) {
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
