package com.advertmarket.communication.channel;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.communication.api.channel.ChatMemberInfo;
import com.advertmarket.communication.api.channel.ChatMemberStatus;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.User;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatMember mapping from Telegram to domain model")
class ChatMemberMappingTest {

    @Test
    @DisplayName("Creator gets all permissions set to true")
    void creator_allPermissionsTrue() {
        ChatMember cm = buildChatMember(
                ChatMember.Status.creator, 100L);

        ChatMemberInfo info =
                TelegramChannelService.mapChatMember(cm);

        assertThat(info.userId()).isEqualTo(100L);
        assertThat(info.status()).isEqualTo(ChatMemberStatus.CREATOR);
        assertThat(info.canPostMessages()).isTrue();
        assertThat(info.canEditMessages()).isTrue();
        assertThat(info.canDeleteMessages()).isTrue();
        assertThat(info.canManageChat()).isTrue();
    }

    @Test
    @DisplayName("Administrator reflects actual permission flags")
    void administrator_reflectsFlags() {
        ChatMember cm = buildChatMember(
                ChatMember.Status.administrator, 200L);
        setField(cm, "can_post_messages", true);
        setField(cm, "can_edit_messages", false);
        setField(cm, "can_delete_messages", true);
        setField(cm, "can_manage_chat", false);

        ChatMemberInfo info =
                TelegramChannelService.mapChatMember(cm);

        assertThat(info.status())
                .isEqualTo(ChatMemberStatus.ADMINISTRATOR);
        assertThat(info.canPostMessages()).isTrue();
        assertThat(info.canEditMessages()).isFalse();
        assertThat(info.canDeleteMessages()).isTrue();
        assertThat(info.canManageChat()).isFalse();
    }

    @Test
    @DisplayName("Administrator with null flags has all false")
    void administrator_nullFlags_allFalse() {
        ChatMember cm = buildChatMember(
                ChatMember.Status.administrator, 201L);

        ChatMemberInfo info =
                TelegramChannelService.mapChatMember(cm);

        assertThat(info.canPostMessages()).isFalse();
        assertThat(info.canEditMessages()).isFalse();
        assertThat(info.canDeleteMessages()).isFalse();
        assertThat(info.canManageChat()).isFalse();
    }

    @Test
    @DisplayName("Regular member has no permissions")
    void member_noPermissions() {
        ChatMember cm = buildChatMember(
                ChatMember.Status.member, 300L);

        ChatMemberInfo info =
                TelegramChannelService.mapChatMember(cm);

        assertThat(info.status()).isEqualTo(ChatMemberStatus.MEMBER);
        assertThat(info.canPostMessages()).isFalse();
        assertThat(info.canEditMessages()).isFalse();
        assertThat(info.canDeleteMessages()).isFalse();
        assertThat(info.canManageChat()).isFalse();
    }

    @Test
    @DisplayName("Restricted user has no permissions")
    void restricted_noPermissions() {
        ChatMember cm = buildChatMember(
                ChatMember.Status.restricted, 400L);

        ChatMemberInfo info =
                TelegramChannelService.mapChatMember(cm);

        assertThat(info.status())
                .isEqualTo(ChatMemberStatus.RESTRICTED);
        assertThat(info.canPostMessages()).isFalse();
    }

    @Test
    @DisplayName("Left user maps correctly")
    void left_mapsCorrectly() {
        ChatMember cm = buildChatMember(
                ChatMember.Status.left, 500L);

        ChatMemberInfo info =
                TelegramChannelService.mapChatMember(cm);

        assertThat(info.status()).isEqualTo(ChatMemberStatus.LEFT);
    }

    @Test
    @DisplayName("Kicked user maps correctly")
    void kicked_mapsCorrectly() {
        ChatMember cm = buildChatMember(
                ChatMember.Status.kicked, 600L);

        ChatMemberInfo info =
                TelegramChannelService.mapChatMember(cm);

        assertThat(info.status()).isEqualTo(ChatMemberStatus.KICKED);
    }

    private static ChatMember buildChatMember(
            ChatMember.Status status, long userId) {
        ChatMember cm = new ChatMember();
        setField(cm, "status", status);

        User user = new User(userId);
        setField(cm, "user", user);

        return cm;
    }

    private static void setField(Object target,
            String fieldName, Object value) {
        try {
            Field field = target.getClass()
                    .getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to set field " + fieldName, e);
        }
    }
}
