package com.advertmarket.communication.bot.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.api.notification.NotificationPort;
import com.advertmarket.communication.api.notification.NotificationRequest;
import com.advertmarket.communication.api.notification.NotificationType;
import com.advertmarket.marketplace.api.dto.ChannelOwnerInfo;
import com.advertmarket.marketplace.api.port.ChannelLifecyclePort;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.ChatMember.Status;
import com.pengrad.telegrambot.model.ChatMemberUpdated;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("BotChannelStatusHandler")
class BotChannelStatusHandlerTest {

    private final ChannelLifecyclePort lifecyclePort =
            mock(ChannelLifecyclePort.class);
    private final NotificationPort notificationPort =
            mock(NotificationPort.class);
    private final MetricsFacade metrics =
            mock(MetricsFacade.class);

    private final BotChannelStatusHandler handler =
            new BotChannelStatusHandler(
                    lifecyclePort, notificationPort, metrics);

    // --- canHandle ---

    @Test
    @DisplayName("Handles channel type updates")
    void canHandle_channelType() throws Exception {
        var update = createUpdate(Chat.Type.channel,
                Status.administrator, Status.left);
        assertThat(handler.canHandle(update)).isTrue();
    }

    @Test
    @DisplayName("Handles supergroup type updates")
    void canHandle_supergroupType() throws Exception {
        var update = createUpdate(Chat.Type.supergroup,
                Status.administrator, Status.member);
        assertThat(handler.canHandle(update)).isTrue();
    }

    @Test
    @DisplayName("Ignores private chat updates")
    void canHandle_ignoresPrivateChat() throws Exception {
        var update = createUpdate(Chat.Type.Private,
                Status.member, Status.left);
        assertThat(handler.canHandle(update)).isFalse();
    }

    @Test
    @DisplayName("Returns false when chat is null")
    void canHandle_nullChat() throws Exception {
        var update = new ChatMemberUpdated();
        assertThat(handler.canHandle(update)).isFalse();
    }

    // --- handle: admin → left (bot removed) ---

    @Test
    @DisplayName("Deactivates channel when bot removed (admin→left)")
    void handle_adminToLeft_deactivatesAndNotifies()
            throws Exception {
        when(lifecyclePort.findOwnerByTelegramId(-100L))
                .thenReturn(Optional.of(
                        new ChannelOwnerInfo(-100L, 42L, "Test")));
        when(lifecyclePort.deactivateByTelegramId(-100L))
                .thenReturn(true);

        handler.handle(createUpdate(Chat.Type.channel,
                Status.administrator, Status.left, -100L));

        verify(lifecyclePort).deactivateByTelegramId(-100L);
        verify(metrics).incrementCounter(
                MetricNames.CHANNEL_DEACTIVATED_TOTAL);

        var captor = ArgumentCaptor.forClass(
                NotificationRequest.class);
        verify(notificationPort).send(captor.capture());
        assertThat(captor.getValue().type())
                .isEqualTo(NotificationType.CHANNEL_BOT_REMOVED);
        assertThat(captor.getValue().recipientUserId())
                .isEqualTo(42L);
    }

    @Test
    @DisplayName("Deactivates channel when bot kicked (admin→kicked)")
    void handle_adminToKicked_deactivatesAndNotifies()
            throws Exception {
        when(lifecyclePort.findOwnerByTelegramId(-200L))
                .thenReturn(Optional.of(
                        new ChannelOwnerInfo(-200L, 99L, "Ch")));

        handler.handle(createUpdate(Chat.Type.channel,
                Status.administrator, Status.kicked, -200L));

        verify(lifecyclePort).deactivateByTelegramId(-200L);
        verify(notificationPort).send(any());
    }

    // --- handle: admin → member (bot demoted) ---

    @Test
    @DisplayName("Deactivates and notifies DEMOTED when admin→member")
    void handle_adminToMember_deactivatesWithDemotedNotification()
            throws Exception {
        when(lifecyclePort.findOwnerByTelegramId(-300L))
                .thenReturn(Optional.of(
                        new ChannelOwnerInfo(-300L, 55L, "Demo")));

        handler.handle(createUpdate(Chat.Type.channel,
                Status.administrator, Status.member, -300L));

        verify(lifecyclePort).deactivateByTelegramId(-300L);
        var captor = ArgumentCaptor.forClass(
                NotificationRequest.class);
        verify(notificationPort).send(captor.capture());
        assertThat(captor.getValue().type())
                .isEqualTo(NotificationType.CHANNEL_BOT_DEMOTED);
    }

    // --- handle: member/left → admin (bot promoted) ---

    @Test
    @DisplayName("Reactivates channel when bot promoted (member→admin)")
    void handle_memberToAdmin_reactivates() throws Exception {
        when(lifecyclePort.findOwnerByTelegramId(-400L))
                .thenReturn(Optional.of(
                        new ChannelOwnerInfo(-400L, 77L, "Promo")));

        handler.handle(createUpdate(Chat.Type.channel,
                Status.member, Status.administrator, -400L));

        verify(lifecyclePort).reactivateByTelegramId(-400L);
        verify(notificationPort, never()).send(any());
    }

    @Test
    @DisplayName("Reactivates channel when bot added as admin (left→admin)")
    void handle_leftToAdmin_reactivates() throws Exception {
        when(lifecyclePort.findOwnerByTelegramId(-500L))
                .thenReturn(Optional.of(
                        new ChannelOwnerInfo(-500L, 88L, "Back")));

        handler.handle(createUpdate(Chat.Type.channel,
                Status.left, Status.administrator, -500L));

        verify(lifecyclePort).reactivateByTelegramId(-500L);
    }

    // --- handle: admin→admin (rights changed) ---

    @Test
    @DisplayName("Deactivates when canPostMessages revoked")
    void handle_adminRightsChanged_canPostFalse_deactivates()
            throws Exception {
        when(lifecyclePort.findOwnerByTelegramId(-600L))
                .thenReturn(Optional.of(
                        new ChannelOwnerInfo(-600L, 11L, "Rights")));

        var update = createUpdateWithCanPost(
                Chat.Type.channel, -600L, false);
        handler.handle(update);

        verify(lifecyclePort).deactivateByTelegramId(-600L);
        verify(notificationPort).send(any());
    }

    @Test
    @DisplayName("Reactivates when canPostMessages granted")
    void handle_adminRightsChanged_canPostTrue_reactivates()
            throws Exception {
        when(lifecyclePort.findOwnerByTelegramId(-700L))
                .thenReturn(Optional.of(
                        new ChannelOwnerInfo(-700L, 22L, "Post")));

        var update = createUpdateWithCanPost(
                Chat.Type.channel, -700L, true);
        handler.handle(update);

        verify(lifecyclePort).reactivateByTelegramId(-700L);
        verify(notificationPort, never()).send(any());
    }

    // --- handle: unregistered channel ---

    @Test
    @DisplayName("Ignores unregistered channel on bot removal")
    void handle_unregisteredChannel_ignored() throws Exception {
        when(lifecyclePort.findOwnerByTelegramId(-999L))
                .thenReturn(Optional.empty());

        handler.handle(createUpdate(Chat.Type.channel,
                Status.administrator, Status.left, -999L));

        verify(lifecyclePort, never())
                .deactivateByTelegramId(anyLong());
        verifyNoInteractions(notificationPort);
    }

    // --- handle: metrics ---

    @Test
    @DisplayName("Records bot status change metric on every handle")
    void handle_recordsStatusChangeMetric() throws Exception {
        when(lifecyclePort.findOwnerByTelegramId(-800L))
                .thenReturn(Optional.empty());

        handler.handle(createUpdate(Chat.Type.channel,
                Status.administrator, Status.left, -800L));

        verify(metrics).incrementCounter(
                eq(MetricNames.CHANNEL_BOT_STATUS_CHANGE));
    }

    // --- Helpers ---

    private ChatMemberUpdated createUpdate(Chat.Type chatType,
            Status oldStatus, Status newStatus) throws Exception {
        return createUpdate(chatType, oldStatus, newStatus, -100L);
    }

    private ChatMemberUpdated createUpdate(Chat.Type chatType,
            Status oldStatus, Status newStatus, long chatId)
            throws Exception {
        var chat = new Chat();
        setField(chat, "id", chatId);
        setField(chat, "type", chatType);

        var oldMember = new ChatMember();
        setField(oldMember, "status", oldStatus);

        var newMember = new ChatMember();
        setField(newMember, "status", newStatus);

        var update = new ChatMemberUpdated();
        setField(update, "chat", chat);
        setField(update, "old_chat_member", oldMember);
        setField(update, "new_chat_member", newMember);
        return update;
    }

    private ChatMemberUpdated createUpdateWithCanPost(
            Chat.Type chatType, long chatId, boolean canPost)
            throws Exception {
        var chat = new Chat();
        setField(chat, "id", chatId);
        setField(chat, "type", chatType);

        var oldMember = new ChatMember();
        setField(oldMember, "status", Status.administrator);

        var newMember = new ChatMember();
        setField(newMember, "status", Status.administrator);
        setField(newMember, "can_post_messages", canPost);

        var update = new ChatMemberUpdated();
        setField(update, "chat", chat);
        setField(update, "old_chat_member", oldMember);
        setField(update, "new_chat_member", newMember);
        return update;
    }

    private static void setField(Object obj, String fieldName,
            Object value) throws Exception {
        Field field = findField(obj.getClass(), fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private static Field findField(Class<?> clazz, String name)
            throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}