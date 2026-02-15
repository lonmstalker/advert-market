package com.advertmarket.marketplace.channel.repository;

import static com.advertmarket.db.generated.tables.Categories.CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;

import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.dto.NewChannel;
import com.advertmarket.marketplace.api.model.ChannelMembershipRole;
import com.advertmarket.marketplace.api.port.CategoryRepository;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import com.advertmarket.marketplace.channel.mapper.ChannelRecordMapper;
import com.advertmarket.marketplace.pricing.mapper.PricingRuleRecordMapper;
import com.advertmarket.marketplace.pricing.repository.JooqPricingRuleRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Implements {@link ChannelRepository} using jOOQ.
 *
 * <p>Delegates record-to-DTO mapping to MapStruct mappers.
 */
@Repository
@RequiredArgsConstructor
public class JooqChannelRepository implements ChannelRepository {

    private final DSLContext dsl;
    private final ChannelRecordMapper channelMapper;
    private final PricingRuleRecordMapper pricingRuleMapper;
    private final CategoryRepository categoryRepository;
    private final JooqPricingRuleRepository pricingRuleRepository;

    @Override
    public boolean existsByTelegramId(long telegramId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CHANNELS)
                        .where(CHANNELS.ID.eq(telegramId)));
    }

    @Override
    @NonNull
    public ChannelResponse insert(@NonNull NewChannel ch) {
        var record = dsl.insertInto(CHANNELS)
                .set(CHANNELS.ID, ch.telegramId())
                .set(CHANNELS.TITLE, ch.title())
                .set(CHANNELS.USERNAME, ch.username())
                .set(CHANNELS.DESCRIPTION, ch.description())
                .set(CHANNELS.SUBSCRIBER_COUNT, ch.subscriberCount())
                .set(CHANNELS.PRICE_PER_POST_NANO, ch.pricePerPostNano())
                .set(CHANNELS.OWNER_ID, ch.ownerId())
                .returning()
                .fetchSingle();

        dsl.insertInto(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.CHANNEL_ID, ch.telegramId())
                .set(CHANNEL_MEMBERSHIPS.USER_ID, ch.ownerId())
                .set(CHANNEL_MEMBERSHIPS.ROLE,
                        ChannelMembershipRole.OWNER.name())
                .execute();

        insertCategories(ch.telegramId(), ch.categories());

        var categories = categoryRepository
                .findCategorySlugsForChannel(ch.telegramId());
        return channelMapper.toResponse(record, categories);
    }

    @Override
    @NonNull
    public Optional<ChannelResponse> findByTelegramId(long telegramId) {
        return dsl.selectFrom(CHANNELS)
                .where(CHANNELS.ID.eq(telegramId))
                .fetchOptional()
                .map(r -> {
                    var categories = categoryRepository
                            .findCategorySlugsForChannel(telegramId);
                    return channelMapper.toResponse(r, categories);
                });
    }

    @Override
    @NonNull
    public Optional<ChannelDetailResponse> findDetailById(long channelId) {
        return dsl.selectFrom(CHANNELS)
                .where(CHANNELS.ID.eq(channelId))
                .fetchOptional()
                .map(ch -> {
                    var categories = categoryRepository
                            .findCategorySlugsForChannel(channelId);
                    var rules = pricingRuleRepository
                            .findByChannelId(channelId);
                    return channelMapper.toDetail(ch, categories, rules);
                });
    }

    @Override
    @NonNull
    public Optional<ChannelResponse> update(long channelId,
                                            @NonNull ChannelUpdateRequest req) {
        var step = dsl.update(CHANNELS)
                .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1));

        if (req.description() != null) {
            step = step.set(CHANNELS.DESCRIPTION, req.description());
        }
        if (req.pricePerPostNano() != null) {
            step = step.set(CHANNELS.PRICE_PER_POST_NANO,
                    req.pricePerPostNano());
        }
        if (req.language() != null) {
            step = step.set(CHANNELS.LANGUAGE, req.language());
        }
        if (req.isActive() != null) {
            step = step.set(CHANNELS.IS_ACTIVE, req.isActive());
        }

        var result = step.where(CHANNELS.ID.eq(channelId))
                .returning()
                .fetchOptional();

        if (result.isEmpty()) {
            return Optional.empty();
        }

        if (req.categories() != null) {
            dsl.deleteFrom(CHANNEL_CATEGORIES)
                    .where(CHANNEL_CATEGORIES.CHANNEL_ID.eq(channelId))
                    .execute();
            insertCategories(channelId, req.categories());
        }

        var categories = categoryRepository
                .findCategorySlugsForChannel(channelId);
        return result.map(r -> channelMapper.toResponse(r, categories));
    }

    @Override
    public boolean deactivate(long channelId) {
        int rows = dsl.update(CHANNELS)
                .set(CHANNELS.IS_ACTIVE, false)
                .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1))
                .where(CHANNELS.ID.eq(channelId))
                .and(CHANNELS.IS_ACTIVE.isTrue())
                .execute();
        return rows > 0;
    }

    private void insertCategories(long channelId, List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return;
        }
        var categoryIds = dsl.select(CATEGORIES.ID)
                .from(CATEGORIES)
                .where(CATEGORIES.SLUG.in(slugs))
                .fetch(CATEGORIES.ID);

        var batch = dsl.insertInto(CHANNEL_CATEGORIES,
                CHANNEL_CATEGORIES.CHANNEL_ID,
                CHANNEL_CATEGORIES.CATEGORY_ID);
        for (var catId : categoryIds) {
            batch = batch.values(channelId, catId);
        }
        batch.onConflictDoNothing().execute();
    }
}
