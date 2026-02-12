package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.PricingRuleCreateRequest;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import com.advertmarket.marketplace.api.dto.PricingRuleUpdateRequest;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Repository port for channel pricing rules.
 */
public interface PricingRuleRepository {

    /** Lists active pricing rules for a channel, ordered by sort_order. */
    @NonNull
    List<PricingRuleDto> findByChannelId(long channelId);

    /** Finds a pricing rule by its ID. */
    @NonNull
    Optional<PricingRuleDto> findById(long ruleId);

    /** Creates a pricing rule. */
    @NonNull
    PricingRuleDto insert(long channelId, @NonNull PricingRuleCreateRequest request);

    /** Updates a pricing rule. Returns the updated rule if found. */
    @NonNull
    Optional<PricingRuleDto> update(long ruleId, @NonNull PricingRuleUpdateRequest request);

    /** Soft-deletes a pricing rule (sets is_active = false). */
    boolean deactivate(long ruleId);
}
