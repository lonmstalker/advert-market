package com.advertmarket.marketplace.adapter;

import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;

import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.ChannelUpdateRequest;
import com.advertmarket.marketplace.api.dto.NewChannel;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import com.advertmarket.marketplace.api.port.ChannelRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link ChannelRepository} using jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqChannelRepository implements ChannelRepository {

    private static final String ROLE_OWNER = "OWNER";

    private final DSLContext dsl;

    @Override
    public boolean existsByTelegramId(long telegramId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CHANNELS)
                        .where(CHANNELS.ID.eq(telegramId)));
    }

    @Override
    @Transactional
    @NonNull
    public ChannelResponse insert(@NonNull NewChannel ch) {
        var record = dsl.insertInto(CHANNELS)
                .set(CHANNELS.ID, ch.telegramId())
                .set(CHANNELS.TITLE, ch.title())
                .set(CHANNELS.USERNAME, ch.username())
                .set(CHANNELS.DESCRIPTION, ch.description())
                .set(CHANNELS.SUBSCRIBER_COUNT, ch.subscriberCount())
                .set(CHANNELS.CATEGORY, ch.category())
                .set(CHANNELS.PRICE_PER_POST_NANO, ch.pricePerPostNano())
                .set(CHANNELS.OWNER_ID, ch.ownerId())
                .returning()
                .fetchSingle();

        dsl.insertInto(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.CHANNEL_ID, ch.telegramId())
                .set(CHANNEL_MEMBERSHIPS.USER_ID, ch.ownerId())
                .set(CHANNEL_MEMBERSHIPS.ROLE, ROLE_OWNER)
                .execute();

        return toResponse(record);
    }

    @Override
    @NonNull
    public Optional<ChannelResponse> findByTelegramId(long telegramId) {
        return dsl.selectFrom(CHANNELS)
                .where(CHANNELS.ID.eq(telegramId))
                .fetchOptional()
                .map(JooqChannelRepository::toResponse);
    }

    @Override
    @NonNull
    public Optional<ChannelDetailResponse> findDetailById(long channelId) {
        return dsl.selectFrom(CHANNELS)
                .where(CHANNELS.ID.eq(channelId))
                .fetchOptional()
                .map(ch -> {
                    List<PricingRuleDto> rules = dsl.selectFrom(CHANNEL_PRICING_RULES)
                            .where(CHANNEL_PRICING_RULES.CHANNEL_ID.eq(channelId))
                            .and(CHANNEL_PRICING_RULES.IS_ACTIVE.isTrue())
                            .orderBy(CHANNEL_PRICING_RULES.SORT_ORDER.asc())
                            .fetch(r -> new PricingRuleDto(
                                    r.getId(),
                                    r.getChannelId(),
                                    r.getName(),
                                    r.getDescription(),
                                    r.getPostType(),
                                    r.getPriceNano(),
                                    r.getIsActive(),
                                    r.getSortOrder()));

                    return new ChannelDetailResponse(
                            ch.getId(),
                            ch.getTitle(),
                            ch.getUsername(),
                            ch.getDescription(),
                            ch.getSubscriberCount(),
                            ch.getCategory(),
                            ch.getPricePerPostNano(),
                            ch.getIsActive(),
                            ch.getOwnerId(),
                            ch.getEngagementRate(),
                            ch.getAvgViews(),
                            ch.getLanguage(),
                            rules,
                            ch.getCreatedAt(),
                            ch.getUpdatedAt());
                });
    }

    @Override
    @Transactional
    @NonNull
    public Optional<ChannelResponse> update(long channelId,
                                            @NonNull ChannelUpdateRequest req) {
        var step = dsl.update(CHANNELS)
                .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1));

        if (req.description() != null) {
            step = step.set(CHANNELS.DESCRIPTION, req.description());
        }
        if (req.category() != null) {
            step = step.set(CHANNELS.CATEGORY, req.category());
        }
        if (req.pricePerPostNano() != null) {
            step = step.set(CHANNELS.PRICE_PER_POST_NANO, req.pricePerPostNano());
        }
        if (req.language() != null) {
            step = step.set(CHANNELS.LANGUAGE, req.language());
        }
        if (req.isActive() != null) {
            step = step.set(CHANNELS.IS_ACTIVE, req.isActive());
        }

        return step.where(CHANNELS.ID.eq(channelId))
                .returning()
                .fetchOptional()
                .map(JooqChannelRepository::toResponse);
    }

    @Override
    @Transactional
    public boolean deactivate(long channelId) {
        int rows = dsl.update(CHANNELS)
                .set(CHANNELS.IS_ACTIVE, false)
                .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1))
                .where(CHANNELS.ID.eq(channelId))
                .and(CHANNELS.IS_ACTIVE.isTrue())
                .execute();
        return rows > 0;
    }

    private static ChannelResponse toResponse(
            com.advertmarket.db.generated.tables.records.ChannelsRecord r) {
        return new ChannelResponse(
                r.getId(),
                r.getTitle(),
                r.getUsername(),
                r.getDescription(),
                r.getSubscriberCount(),
                r.getCategory(),
                r.getPricePerPostNano(),
                r.getIsActive(),
                r.getOwnerId(),
                r.getCreatedAt());
    }
}
