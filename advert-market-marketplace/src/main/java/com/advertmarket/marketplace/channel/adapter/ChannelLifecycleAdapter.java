package com.advertmarket.marketplace.channel.adapter;

import static com.advertmarket.db.generated.tables.Channels.CHANNELS;

import com.advertmarket.marketplace.api.dto.ChannelOwnerInfo;
import com.advertmarket.marketplace.api.port.ChannelLifecyclePort;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Implements {@link ChannelLifecyclePort} using jOOQ with
 * optimistic locking via the {@code version} column.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelLifecycleAdapter implements ChannelLifecyclePort {

    private final DSLContext dsl;

    @Override
    public boolean deactivateByTelegramId(long telegramChannelId) {
        int rows = dsl.update(CHANNELS)
                .set(CHANNELS.IS_ACTIVE, false)
                .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1))
                .set(CHANNELS.UPDATED_AT, OffsetDateTime.now())
                .where(CHANNELS.ID.eq(telegramChannelId))
                .and(CHANNELS.IS_ACTIVE.isTrue())
                .execute();
        if (rows > 0) {
            log.info("Deactivated channel telegram_id={}",
                    telegramChannelId);
        }
        return rows > 0;
    }

    @Override
    public boolean reactivateByTelegramId(long telegramChannelId) {
        int rows = dsl.update(CHANNELS)
                .set(CHANNELS.IS_ACTIVE, true)
                .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1))
                .set(CHANNELS.UPDATED_AT, OffsetDateTime.now())
                .where(CHANNELS.ID.eq(telegramChannelId))
                .and(CHANNELS.IS_ACTIVE.isFalse())
                .execute();
        if (rows > 0) {
            log.info("Reactivated channel telegram_id={}",
                    telegramChannelId);
        }
        return rows > 0;
    }

    @Override
    @NonNull
    public Optional<ChannelOwnerInfo> findOwnerByTelegramId(
            long telegramChannelId) {
        return dsl.select(
                        CHANNELS.ID,
                        CHANNELS.OWNER_ID,
                        CHANNELS.TITLE)
                .from(CHANNELS)
                .where(CHANNELS.ID.eq(telegramChannelId))
                .fetchOptional(r -> new ChannelOwnerInfo(
                        r.get(CHANNELS.ID),
                        r.get(CHANNELS.OWNER_ID),
                        r.get(CHANNELS.TITLE)));
    }
}