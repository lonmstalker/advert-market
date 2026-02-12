package com.advertmarket.marketplace.web;

import com.advertmarket.marketplace.api.dto.ChannelRegistrationRequest;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelVerifyRequest;
import com.advertmarket.marketplace.api.dto.ChannelVerifyResponse;
import com.advertmarket.marketplace.service.ChannelRegistrationService;
import com.advertmarket.shared.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Channel verification and registration endpoints.
 */
@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
@Tag(name = "Channels", description = "Channel registration")
@SecurityRequirement(name = "bearerAuth")
public class ChannelController {

    private final ChannelRegistrationService registrationService;

    /**
     * Verifies a channel: checks bot admin status and user ownership.
     */
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
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
