package com.advertmarket.marketplace.pricing.web;

import com.advertmarket.marketplace.api.dto.PricingRuleCreateRequest;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import com.advertmarket.marketplace.api.dto.PricingRuleUpdateRequest;
import com.advertmarket.marketplace.pricing.service.PricingRuleService;
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
 * Pricing rule CRUD endpoints.
 */
@RestController
@RequestMapping("/api/v1/channels/{channelId}/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing Rules", description = "Channel pricing rule management")
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;

    /**
     * Lists active pricing rules for a channel.
     */
    @GetMapping
    @Operation(summary = "List pricing rules",
            description = "Lists active pricing rules for a channel")
    @ApiResponse(responseCode = "200", description = "List of pricing rules")
    public List<PricingRuleDto> list(@PathVariable("channelId") long channelId) {
        return pricingRuleService.listByChannel(channelId);
    }

    /**
     * Creates a pricing rule. Owner only.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create pricing rule",
            description = "Creates a pricing rule for a channel (owner only)")
    @ApiResponse(responseCode = "201", description = "Rule created")
    @ApiResponse(responseCode = "403",
            description = "Not the owner of the channel")
    public PricingRuleDto create(
            @PathVariable("channelId") long channelId,
            @Valid @RequestBody PricingRuleCreateRequest request) {
        return pricingRuleService.create(channelId, request);
    }

    /**
     * Updates a pricing rule. Owner only.
     */
    @PutMapping("/{ruleId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update pricing rule",
            description = "Updates a pricing rule (owner only)")
    @ApiResponse(responseCode = "200", description = "Rule updated")
    @ApiResponse(responseCode = "403",
            description = "Not the owner of the channel")
    @ApiResponse(responseCode = "404",
            description = "Pricing rule not found")
    public PricingRuleDto update(
            @PathVariable("channelId") long channelId,
            @PathVariable("ruleId") long ruleId,
            @Valid @RequestBody PricingRuleUpdateRequest request) {
        return pricingRuleService.update(channelId, ruleId, request);
    }

    /**
     * Soft-deletes a pricing rule. Owner only.
     */
    @DeleteMapping("/{ruleId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete pricing rule",
            description = "Soft-deletes a pricing rule (owner only)")
    @ApiResponse(responseCode = "204", description = "Rule deleted")
    @ApiResponse(responseCode = "403",
            description = "Not the owner of the channel")
    @ApiResponse(responseCode = "404",
            description = "Pricing rule not found")
    public void delete(
            @PathVariable("channelId") long channelId,
            @PathVariable("ruleId") long ruleId) {
        pricingRuleService.delete(channelId, ruleId);
    }
}
