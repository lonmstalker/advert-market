package com.advertmarket.marketplace.pricing.repository;

import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.PricingRulePostTypes.PRICING_RULE_POST_TYPES;

import com.advertmarket.marketplace.api.dto.PricingRuleCreateRequest;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import com.advertmarket.marketplace.api.dto.PricingRuleUpdateRequest;
import com.advertmarket.marketplace.api.model.PostType;
import com.advertmarket.marketplace.api.port.PricingRuleRepository;
import com.advertmarket.marketplace.pricing.mapper.PricingRuleRecordMapper;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Implements {@link PricingRuleRepository} using jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqPricingRuleRepository implements PricingRuleRepository {

    private final DSLContext dsl;
    private final PricingRuleRecordMapper mapper;

    @Override
    @NonNull
    public List<PricingRuleDto> findByChannelId(long channelId) {
        var records = dsl.selectFrom(CHANNEL_PRICING_RULES)
                .where(CHANNEL_PRICING_RULES.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_PRICING_RULES.IS_ACTIVE.isTrue())
                .orderBy(CHANNEL_PRICING_RULES.SORT_ORDER.asc())
                .fetch();

        if (records.isEmpty()) {
            return List.of();
        }

        var ruleIds = records.map(r -> r.getId());
        var postTypesMap = fetchPostTypesMap(ruleIds);

        return records.stream()
                .map(r -> mapper.toDto(r,
                        postTypesMap.getOrDefault(r.getId(), Set.of())))
                .toList();
    }

    @Override
    @NonNull
    public Optional<PricingRuleDto> findById(long ruleId) {
        return dsl.selectFrom(CHANNEL_PRICING_RULES)
                .where(CHANNEL_PRICING_RULES.ID.eq(ruleId))
                .fetchOptional()
                .map(r -> mapper.toDto(r, fetchPostTypes(ruleId)));
    }

    @Override
    @NonNull
    public PricingRuleDto insert(long channelId,
                                 @NonNull PricingRuleCreateRequest req) {
        var record = dsl.insertInto(CHANNEL_PRICING_RULES)
                .set(CHANNEL_PRICING_RULES.CHANNEL_ID, channelId)
                .set(CHANNEL_PRICING_RULES.NAME, req.name())
                .set(CHANNEL_PRICING_RULES.DESCRIPTION, req.description())
                .set(CHANNEL_PRICING_RULES.PRICE_NANO, req.priceNano())
                .set(CHANNEL_PRICING_RULES.SORT_ORDER, req.sortOrder())
                .returning()
                .fetchSingle();

        insertPostTypes(record.getId(), req.postTypes());

        return mapper.toDto(record, req.postTypes());
    }

    @Override
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

        var result = step.where(CHANNEL_PRICING_RULES.ID.eq(ruleId))
                .returning()
                .fetchOptional();

        if (result.isEmpty()) {
            return Optional.empty();
        }

        if (req.postTypes() != null) {
            dsl.deleteFrom(PRICING_RULE_POST_TYPES)
                    .where(PRICING_RULE_POST_TYPES.PRICING_RULE_ID
                            .eq(ruleId))
                    .execute();
            insertPostTypes(ruleId, req.postTypes());
        }

        var postTypes = fetchPostTypes(ruleId);
        return result.map(r -> mapper.toDto(r, postTypes));
    }

    @Override
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

    private void insertPostTypes(long ruleId,
                                 Set<PostType> postTypes) {
        if (postTypes == null || postTypes.isEmpty()) {
            return;
        }
        var batch = dsl.insertInto(PRICING_RULE_POST_TYPES,
                PRICING_RULE_POST_TYPES.PRICING_RULE_ID,
                PRICING_RULE_POST_TYPES.POST_TYPE);
        for (var pt : postTypes) {
            batch = batch.values(ruleId, pt.name());
        }
        batch.execute();
    }

    private Set<PostType> fetchPostTypes(long ruleId) {
        return dsl.select(PRICING_RULE_POST_TYPES.POST_TYPE)
                .from(PRICING_RULE_POST_TYPES)
                .where(PRICING_RULE_POST_TYPES.PRICING_RULE_ID.eq(ruleId))
                .fetchSet(r -> PostType.valueOf(
                        r.get(PRICING_RULE_POST_TYPES.POST_TYPE)));
    }

    private Map<Long, Set<PostType>> fetchPostTypesMap(
            List<Long> ruleIds) {
        return dsl.select(
                        PRICING_RULE_POST_TYPES.PRICING_RULE_ID,
                        PRICING_RULE_POST_TYPES.POST_TYPE)
                .from(PRICING_RULE_POST_TYPES)
                .where(PRICING_RULE_POST_TYPES.PRICING_RULE_ID.in(ruleIds))
                .fetchGroups(
                        r -> r.get(PRICING_RULE_POST_TYPES.PRICING_RULE_ID),
                        r -> PostType.valueOf(
                                r.get(PRICING_RULE_POST_TYPES.POST_TYPE)))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> EnumSet.copyOf(e.getValue())));
    }
}
