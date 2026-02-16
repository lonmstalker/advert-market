package com.advertmarket.marketplace.pricing.service;

import com.advertmarket.marketplace.api.dto.PricingRuleCreateRequest;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import com.advertmarket.marketplace.api.dto.PricingRuleUpdateRequest;
import com.advertmarket.marketplace.api.port.ChannelAuthorizationPort;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.marketplace.api.port.PricingRuleRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD operations for channel pricing rules.
 */
@Service
@RequiredArgsConstructor
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final ChannelAuthorizationPort authorizationPort;
    private final ChannelAutoSyncPort channelAutoSyncPort;

    /**
     * Lists active pricing rules for a channel.
     *
     * @param channelId channel ID
     * @return list of pricing rules
     */
    @NonNull
    public List<PricingRuleDto> listByChannel(long channelId) {
        return pricingRuleRepository.findByChannelId(channelId);
    }

    /**
     * Creates a pricing rule for a channel. Owner only.
     *
     * @param channelId channel ID
     * @param request   rule data
     * @return created rule
     */
    @NonNull
    @Transactional
    public PricingRuleDto create(long channelId,
                                 @NonNull PricingRuleCreateRequest request) {
        requireOwner(channelId);
        channelAutoSyncPort.syncFromTelegram(channelId);
        return pricingRuleRepository.insert(channelId, request);
    }

    /**
     * Updates a pricing rule. Owner only.
     *
     * @param channelId channel ID (for authorization)
     * @param ruleId    rule ID
     * @param request   fields to update
     * @return updated rule
     */
    @NonNull
    @Transactional
    public PricingRuleDto update(long channelId, long ruleId,
                                 @NonNull PricingRuleUpdateRequest request) {
        requireOwner(channelId);
        channelAutoSyncPort.syncFromTelegram(channelId);
        return pricingRuleRepository.update(ruleId, request)
                .orElseThrow(() -> new DomainException(
                        ErrorCodes.PRICING_RULE_NOT_FOUND,
                        "Pricing rule not found: " + ruleId));
    }

    /**
     * Soft-deletes a pricing rule. Owner only.
     *
     * @param channelId channel ID (for authorization)
     * @param ruleId    rule ID
     */
    @Transactional
    public void delete(long channelId, long ruleId) {
        requireOwner(channelId);
        channelAutoSyncPort.syncFromTelegram(channelId);
        boolean deleted = pricingRuleRepository.deactivate(ruleId);
        if (!deleted) {
            throw new DomainException(
                    ErrorCodes.PRICING_RULE_NOT_FOUND,
                    "Pricing rule not found: " + ruleId);
        }
    }

    private void requireOwner(long channelId) {
        if (!authorizationPort.isOwner(channelId)) {
            throw new DomainException(
                    ErrorCodes.CHANNEL_NOT_OWNED,
                    "Not the owner of channel: " + channelId);
        }
    }
}
