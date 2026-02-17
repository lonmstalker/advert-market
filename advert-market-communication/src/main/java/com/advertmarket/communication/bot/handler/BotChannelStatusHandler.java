package com.advertmarket.communication.bot.handler;

import com.advertmarket.communication.api.notification.NotificationPort;
import com.advertmarket.communication.api.notification.NotificationRequest;
import com.advertmarket.communication.api.notification.NotificationType;
import com.advertmarket.communication.bot.internal.dispatch.ChatMemberUpdateHandler;
import com.advertmarket.marketplace.api.dto.ChannelOwnerInfo;
import com.advertmarket.marketplace.api.port.ChannelLifecyclePort;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.ChatMember.Status;
import com.pengrad.telegrambot.model.ChatMemberUpdated;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles {@code my_chat_member} updates for channels:
 * deactivates/reactivates channels when the bot is
 * removed, demoted, or re-promoted.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BotChannelStatusHandler implements ChatMemberUpdateHandler {

    private final ChannelLifecyclePort channelLifecyclePort;
    private final NotificationPort notificationPort;
    private final MetricsFacade metrics;

    @Override
    public boolean canHandle(ChatMemberUpdated update) {
        if (update.chat() == null
                || update.oldChatMember() == null
                || update.newChatMember() == null) {
            return false;
        }
        Chat.Type type = update.chat().type();
        return type == Chat.Type.channel
                || type == Chat.Type.supergroup;
    }

    @Override
    public void handle(ChatMemberUpdated update) {
        long channelId = update.chat().id();
        Status oldStatus = update.oldChatMember().status();
        Status newStatus = update.newChatMember().status();
        ChatMember newMember = update.newChatMember();

        metrics.incrementCounter(
                MetricNames.CHANNEL_BOT_STATUS_CHANGE);
        log.info("Bot status change in channel={}: {} -> {}",
                channelId, oldStatus, newStatus);

        if (isAdmin(oldStatus)
                && (newStatus == Status.left
                        || newStatus == Status.kicked)) {
            handleBotRemoved(channelId);
        } else if (isAdmin(oldStatus)
                && newStatus == Status.member) {
            handleBotDemoted(channelId);
        } else if (!isAdmin(oldStatus)
                && isAdmin(newStatus)) {
            handleBotPromoted(channelId);
        } else if (isAdmin(oldStatus) && isAdmin(newStatus)) {
            handleAdminRightsChanged(channelId, newMember);
        }
    }

    private void handleBotRemoved(long channelId) {
        Optional<ChannelOwnerInfo> owner =
                channelLifecyclePort.findOwnerByTelegramId(channelId);
        if (owner.isEmpty()) {
            log.debug("Unregistered channel={}, ignoring removal",
                    channelId);
            return;
        }
        boolean deactivated =
                channelLifecyclePort.deactivateByTelegramId(channelId);
        if (!deactivated) {
            log.debug("Channel={} already inactive on removal update",
                    channelId);
            return;
        }
        metrics.incrementCounter(
                MetricNames.CHANNEL_DEACTIVATED_TOTAL);
        notifyOwner(owner.get(),
                NotificationType.CHANNEL_BOT_REMOVED);
    }

    private void handleBotDemoted(long channelId) {
        Optional<ChannelOwnerInfo> owner =
                channelLifecyclePort.findOwnerByTelegramId(channelId);
        if (owner.isEmpty()) {
            log.debug("Unregistered channel={}, ignoring demotion",
                    channelId);
            return;
        }
        boolean deactivated =
                channelLifecyclePort.deactivateByTelegramId(channelId);
        if (!deactivated) {
            log.debug("Channel={} already inactive on demotion update",
                    channelId);
            return;
        }
        metrics.incrementCounter(
                MetricNames.CHANNEL_DEACTIVATED_TOTAL);
        notifyOwner(owner.get(),
                NotificationType.CHANNEL_BOT_DEMOTED);
    }

    private void handleBotPromoted(long channelId) {
        Optional<ChannelOwnerInfo> owner =
                channelLifecyclePort.findOwnerByTelegramId(channelId);
        if (owner.isEmpty()) {
            log.debug("Unregistered channel={}, ignoring promotion",
                    channelId);
            return;
        }
        boolean reactivated =
                channelLifecyclePort.reactivateByTelegramId(channelId);
        if (reactivated) {
            notifyOwner(owner.get(),
                    NotificationType.CHANNEL_BOT_RESTORED);
            log.info("Channel={} reactivated after bot promotion",
                    channelId);
            return;
        }
        log.debug("Channel={} already active on promotion update",
                channelId);
    }

    private void handleAdminRightsChanged(long channelId,
            ChatMember newMember) {
        boolean canPost =
                Boolean.TRUE.equals(newMember.canPostMessages());
        Optional<ChannelOwnerInfo> owner =
                channelLifecyclePort.findOwnerByTelegramId(channelId);
        if (owner.isEmpty()) {
            log.debug("Unregistered channel={}, ignoring rights "
                    + "change", channelId);
            return;
        }
        if (canPost) {
            boolean reactivated =
                    channelLifecyclePort.reactivateByTelegramId(channelId);
            if (reactivated) {
                notifyOwner(owner.get(),
                        NotificationType.CHANNEL_BOT_RESTORED);
                log.info("Channel={} reactivated: canPostMessages=true",
                        channelId);
                return;
            }
            log.debug("Channel={} already active on rights update",
                    channelId);
        } else {
            boolean deactivated =
                    channelLifecyclePort.deactivateByTelegramId(channelId);
            if (!deactivated) {
                log.debug("Channel={} already inactive on rights update",
                        channelId);
                return;
            }
            metrics.incrementCounter(
                    MetricNames.CHANNEL_DEACTIVATED_TOTAL);
            notifyOwner(owner.get(),
                    NotificationType.CHANNEL_BOT_DEMOTED);
        }
    }

    private void notifyOwner(ChannelOwnerInfo info,
            NotificationType type) {
        notificationPort.send(new NotificationRequest(
                info.ownerId(), type,
                Map.of("channel_name", info.title())));
    }

    private static boolean isAdmin(Status status) {
        return status == Status.administrator
                || status == Status.creator;
    }
}
