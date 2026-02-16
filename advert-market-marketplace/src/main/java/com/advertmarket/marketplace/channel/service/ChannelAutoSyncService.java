package com.advertmarket.marketplace.channel.service;

import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_INACCESSIBLE;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_NOT_FOUND;

import com.advertmarket.marketplace.api.dto.ChannelSyncResult;
import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberStatus;
import com.advertmarket.marketplace.api.model.ChannelMembershipRole;
import com.advertmarket.marketplace.api.port.ChannelAutoSyncPort;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.shared.exception.DomainException;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Synchronizes channel owner/admin state from Telegram into local storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelAutoSyncService implements ChannelAutoSyncPort {

    private static final String TYPE_CHANNEL = "channel";
    private static final String SOURCE_TELEGRAM_SYNC = "TELEGRAM_SYNC";

    private static final JSONB EMPTY_RIGHTS = JSONB.valueOf("{}");

    private final TelegramChannelPort telegramChannelPort;
    private final DSLContext dsl;

    @Override
    @Transactional
    @NonNull
    public ChannelSyncResult syncFromTelegram(long channelId) {
        ChatInfo chatInfo = telegramChannelPort.getChat(channelId);
        if (!TYPE_CHANNEL.equalsIgnoreCase(chatInfo.type())) {
            throw new DomainException(
                    CHANNEL_NOT_FOUND,
                    "Chat " + channelId + " is not a channel, type: "
                            + chatInfo.type());
        }

        List<ChatMemberInfo> administrators =
                telegramChannelPort.getChatAdministrators(channelId);
        int memberCount = telegramChannelPort.getChatMemberCount(channelId);

        long newOwnerId = resolveCreatorOwnerId(channelId, administrators);
        upsertUsers(administrators);

        Long oldOwnerId = dsl.select(CHANNELS.OWNER_ID)
                .from(CHANNELS)
                .where(CHANNELS.ID.eq(channelId))
                .fetchOne(CHANNELS.OWNER_ID);

        upsertChannel(chatInfo, memberCount, newOwnerId);
        syncMemberships(channelId, newOwnerId, administrators);

        boolean ownerChanged = oldOwnerId != null
                && oldOwnerId != newOwnerId;
        if (ownerChanged) {
            log.info("Channel={} owner transferred: {} -> {}",
                    channelId, oldOwnerId, newOwnerId);
        }

        return new ChannelSyncResult(ownerChanged, oldOwnerId, newOwnerId);
    }

    private long resolveCreatorOwnerId(
            long channelId,
            List<ChatMemberInfo> administrators) {
        var creators = administrators.stream()
                .filter(a -> a.status() == ChatMemberStatus.CREATOR)
                .map(ChatMemberInfo::userId)
                .distinct()
                .toList();
        if (creators.size() != 1) {
            throw new DomainException(
                    CHANNEL_INACCESSIBLE,
                    "Unable to resolve unique channel creator for "
                            + channelId);
        }
        return creators.getFirst();
    }

    private void upsertUsers(List<ChatMemberInfo> administrators) {
        Set<Long> userIds = administrators.stream()
                .map(ChatMemberInfo::userId)
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new));
        if (userIds.isEmpty()) {
            return;
        }

        var insert = dsl.insertInto(USERS, USERS.ID, USERS.FIRST_NAME);
        for (Long userId : userIds) {
            insert = insert.values(userId, "telegram-user-" + userId);
        }
        insert.onConflict(USERS.ID).doNothing().execute();
    }

    private void upsertChannel(
            ChatInfo chatInfo,
            int memberCount,
            long ownerId) {
        var now = OffsetDateTime.now();
        dsl.insertInto(CHANNELS)
                .set(CHANNELS.ID, chatInfo.id())
                .set(CHANNELS.TITLE, chatInfo.title())
                .set(CHANNELS.USERNAME, chatInfo.username())
                .set(CHANNELS.DESCRIPTION, chatInfo.description())
                .set(CHANNELS.SUBSCRIBER_COUNT, memberCount)
                .set(CHANNELS.OWNER_ID, ownerId)
                .set(CHANNELS.IS_ACTIVE, true)
                .onConflict(CHANNELS.ID)
                .doUpdate()
                .set(CHANNELS.TITLE, chatInfo.title())
                .set(CHANNELS.USERNAME, chatInfo.username())
                .set(CHANNELS.DESCRIPTION, chatInfo.description())
                .set(CHANNELS.SUBSCRIBER_COUNT, memberCount)
                .set(CHANNELS.OWNER_ID, ownerId)
                .set(CHANNELS.IS_ACTIVE, true)
                .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1))
                .set(CHANNELS.UPDATED_AT, now)
                .execute();
    }

    private void syncMemberships(
            long channelId,
            long ownerId,
            List<ChatMemberInfo> administrators) {
        var adminIds = administrators.stream()
                .map(ChatMemberInfo::userId)
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new));

        var managerIds = new LinkedHashSet<>(adminIds);
        managerIds.remove(ownerId);

        // Replace stale owner rows first to keep owner uniqueness invariant.
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS)
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_MEMBERSHIPS.ROLE.eq(
                        ChannelMembershipRole.OWNER.name()))
                .and(CHANNEL_MEMBERSHIPS.USER_ID.ne(ownerId))
                .execute();

        dsl.insertInto(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.CHANNEL_ID, channelId)
                .set(CHANNEL_MEMBERSHIPS.USER_ID, ownerId)
                .set(CHANNEL_MEMBERSHIPS.ROLE,
                        ChannelMembershipRole.OWNER.name())
                .set(CHANNEL_MEMBERSHIPS.RIGHTS, EMPTY_RIGHTS)
                .set(CHANNEL_MEMBERSHIPS.SOURCE, SOURCE_TELEGRAM_SYNC)
                .onConflict(
                        CHANNEL_MEMBERSHIPS.CHANNEL_ID,
                        CHANNEL_MEMBERSHIPS.USER_ID)
                .doUpdate()
                .set(CHANNEL_MEMBERSHIPS.ROLE,
                        ChannelMembershipRole.OWNER.name())
                .set(CHANNEL_MEMBERSHIPS.RIGHTS, EMPTY_RIGHTS)
                .set(CHANNEL_MEMBERSHIPS.SOURCE, SOURCE_TELEGRAM_SYNC)
                .set(CHANNEL_MEMBERSHIPS.VERSION,
                        CHANNEL_MEMBERSHIPS.VERSION.plus(1))
                .execute();

        for (Long managerId : managerIds) {
            dsl.insertInto(CHANNEL_MEMBERSHIPS)
                    .set(CHANNEL_MEMBERSHIPS.CHANNEL_ID, channelId)
                    .set(CHANNEL_MEMBERSHIPS.USER_ID, managerId)
                    .set(CHANNEL_MEMBERSHIPS.ROLE,
                            ChannelMembershipRole.MANAGER.name())
                    .set(CHANNEL_MEMBERSHIPS.RIGHTS, EMPTY_RIGHTS)
                    .set(CHANNEL_MEMBERSHIPS.SOURCE, SOURCE_TELEGRAM_SYNC)
                    .onConflict(
                            CHANNEL_MEMBERSHIPS.CHANNEL_ID,
                            CHANNEL_MEMBERSHIPS.USER_ID)
                    .doUpdate()
                    .set(CHANNEL_MEMBERSHIPS.ROLE,
                            ChannelMembershipRole.MANAGER.name())
                    .set(CHANNEL_MEMBERSHIPS.RIGHTS, EMPTY_RIGHTS)
                    .set(CHANNEL_MEMBERSHIPS.SOURCE, SOURCE_TELEGRAM_SYNC)
                    .set(CHANNEL_MEMBERSHIPS.VERSION,
                            CHANNEL_MEMBERSHIPS.VERSION.plus(1))
                    .where(CHANNEL_MEMBERSHIPS.SOURCE.eq(
                            SOURCE_TELEGRAM_SYNC))
                    .execute();
        }

        var staleDelete = dsl.deleteFrom(CHANNEL_MEMBERSHIPS)
                .where(CHANNEL_MEMBERSHIPS.CHANNEL_ID.eq(channelId))
                .and(CHANNEL_MEMBERSHIPS.ROLE.eq(
                        ChannelMembershipRole.MANAGER.name()))
                .and(CHANNEL_MEMBERSHIPS.SOURCE.eq(
                        SOURCE_TELEGRAM_SYNC));
        if (!managerIds.isEmpty()) {
            staleDelete = staleDelete.and(
                    CHANNEL_MEMBERSHIPS.USER_ID.notIn(managerIds));
        }
        staleDelete.execute();
    }
}
