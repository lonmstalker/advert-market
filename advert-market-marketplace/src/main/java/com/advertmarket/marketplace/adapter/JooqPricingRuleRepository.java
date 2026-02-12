package com.advertmarket.marketplace.adapter;

import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;

import com.advertmarket.marketplace.api.dto.PricingRuleCreateRequest;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import com.advertmarket.marketplace.api.dto.PricingRuleUpdateRequest;
import com.advertmarket.marketplace.api.port.PricingRuleRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link PricingRuleRepository} using jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqPricingRuleRepository implements PricingRuleRepository {

    private final DSLContext dsl;

    @Override
    @NonNull
    public List<PricingRuleDto> findByChannelId(long channelId) {
        return dsl.selectFrom(CHANNEL_PRICING_RULES)
                .where(CHANNEL_PRICING_RULES.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_PRICING_RULES.IS_ACTIVE.isTrue())
                .orderBy(CHANNEL_PRICING_RULES.SORT_ORDER.asc())
                .fetch(JooqPricingRuleRepository::toDto);
    }

    @Override
    @NonNull
    public Optional<PricingRuleDto> findById(long ruleId) {
        return dsl.selectFrom(CHANNEL_PRICING_RULES)
                .where(CHANNEL_PRICING_RULES.ID.eq(ruleId))
                .fetchOptional()
                .map(JooqPricingRuleRepository::toDto);
    }

    @Override
    @Transactional
    @NonNull
    public PricingRuleDto insert(long channelId,
                                 @NonNull PricingRuleCreateRequest req) {
        var record = dsl.insertInto(CHANNEL_PRICING_RULES)
                .set(CHANNEL_PRICING_RULES.CHANNEL_ID, channelId)
                .set(CHANNEL_PRICING_RULES.NAME, req.name())
                .set(CHANNEL_PRICING_RULES.DESCRIPTION, req.description())
                .set(CHANNEL_PRICING_RULES.POST_TYPE, req.postType())
                .set(CHANNEL_PRICING_RULES.PRICE_NANO, req.priceNano())
                .set(CHANNEL_PRICING_RULES.SORT_ORDER, req.sortOrder())
                .returning()
                .fetchSingle();
        return toDto(record);
    }

    @Override
    @Transactional
    @NonNull
    public Optional<PricingRuleDto> update(
            long ruleId, @NonNull PricingRuleUpdateRequest req) {

        var step = dsl.update(CHANNEL_PRICING_RULES)
                .set(CHANNEL_PRICING_RULES.VERSION,
                        CHANNEL_PRICING_RULES.VERSION.plus(1));

        if (req.name() != null) {
            step = step.set(CHANNEL_PRICING_RULES.NAME, req.name());
        }
        if (req.description() != null) {
            step = step.set(CHANNEL_PRICING_RULES.DESCRIPTION,
                    req.description());
        }
        if (req.priceNano() != null) {
            step = step.set(CHANNEL_PRICING_RULES.PRICE_NANO,
                    req.priceNano());
        }
        if (req.sortOrder() != null) {
            step = step.set(CHANNEL_PRICING_RULES.SORT_ORDER,
                    req.sortOrder());
        }
        if (req.isActive() != null) {
            step = step.set(CHANNEL_PRICING_RULES.IS_ACTIVE,
                    req.isActive());
        }

        return step.where(CHANNEL_PRICING_RULES.ID.eq(ruleId))
                .returning()
                .fetchOptional()
                .map(JooqPricingRuleRepository::toDto);
    }

    @Override
    @Transactional
    public boolean deactivate(long ruleId) {
        int rows = dsl.update(CHANNEL_PRICING_RULES)
                .set(CHANNEL_PRICING_RULES.IS_ACTIVE, false)
                .set(CHANNEL_PRICING_RULES.VERSION,
                        CHANNEL_PRICING_RULES.VERSION.plus(1))
                .where(CHANNEL_PRICING_RULES.ID.eq(ruleId))
                .and(CHANNEL_PRICING_RULES.IS_ACTIVE.isTrue())
                .execute();
        return rows > 0;
    }

    private static PricingRuleDto toDto(
            com.advertmarket.db.generated.tables.records
                    .ChannelPricingRulesRecord r) {
        return new PricingRuleDto(
                r.getId(),
                r.getChannelId(),
                r.getName(),
                r.getDescription(),
                r.getPostType(),
                r.getPriceNano(),
                r.getIsActive(),
                r.getSortOrder());
    }
}
