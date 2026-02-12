package com.advertmarket.marketplace.adapter;

import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;

import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.NewChannel;
import com.advertmarket.marketplace.api.port.ChannelRepository;
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

        return new ChannelResponse(
                record.getId(),
                record.getTitle(),
                record.getUsername(),
                record.getDescription(),
                record.getSubscriberCount(),
                record.getCategory(),
                record.getPricePerPostNano(),
                record.getIsActive(),
                record.getOwnerId(),
                record.getCreatedAt());
    }

    @Override
    @NonNull
    public Optional<ChannelResponse> findByTelegramId(long telegramId) {
        return dsl.selectFrom(CHANNELS)
                .where(CHANNELS.ID.eq(telegramId))
                .fetchOptional()
                .map(r -> new ChannelResponse(
                        r.getId(),
                        r.getTitle(),
                        r.getUsername(),
                        r.getDescription(),
                        r.getSubscriberCount(),
                        r.getCategory(),
                        r.getPricePerPostNano(),
                        r.getIsActive(),
                        r.getOwnerId(),
                        r.getCreatedAt()));
    }
}
