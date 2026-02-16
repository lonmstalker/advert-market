package com.advertmarket.marketplace.channel.service;

import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_INACCESSIBLE;
import static com.advertmarket.shared.exception.ErrorCodes.CHANNEL_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.marketplace.api.dto.telegram.ChatInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberStatus;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.shared.exception.DomainException;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ChannelAutoSyncService — Telegram channel sync logic")
@ExtendWith(MockitoExtension.class)
class ChannelAutoSyncServiceTest {

    private static final long CHANNEL_ID = -1001234567890L;
    private static final long CREATOR_ID = 100L;
    private static final long ADMIN_ID = 200L;
    private static final long OLD_OWNER_ID = 300L;
    private static final int MEMBER_COUNT = 5000;

    @Mock
    private TelegramChannelPort telegramChannelPort;

    private final DSLContext dsl =
            mock(DSLContext.class, Answers.RETURNS_DEEP_STUBS);

    private ChannelAutoSyncService service;

    @BeforeEach
    void setUp() {
        service = new ChannelAutoSyncService(telegramChannelPort, dsl);
    }

    @Nested
    @DisplayName("syncFromTelegram — validation")
    class Validation {

        @Test
        @DisplayName("Should throw CHANNEL_NOT_FOUND when chat type is not channel")
        void rejectsNonChannelType() {
            when(telegramChannelPort.getChat(CHANNEL_ID))
                    .thenReturn(chatInfo("supergroup"));

            assertThatThrownBy(() -> service.syncFromTelegram(CHANNEL_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(CHANNEL_NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw CHANNEL_INACCESSIBLE when no creator found")
        void rejectsNoCreator() {
            when(telegramChannelPort.getChat(CHANNEL_ID))
                    .thenReturn(chatInfo("channel"));
            when(telegramChannelPort.getChatAdministrators(CHANNEL_ID))
                    .thenReturn(List.of(
                            adminMember(ADMIN_ID)));

            assertThatThrownBy(() -> service.syncFromTelegram(CHANNEL_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(CHANNEL_INACCESSIBLE);
        }

        @Test
        @DisplayName("Should throw CHANNEL_INACCESSIBLE when multiple creators")
        void rejectsMultipleCreators() {
            when(telegramChannelPort.getChat(CHANNEL_ID))
                    .thenReturn(chatInfo("channel"));
            when(telegramChannelPort.getChatAdministrators(CHANNEL_ID))
                    .thenReturn(List.of(
                            creatorMember(CREATOR_ID),
                            creatorMember(ADMIN_ID)));

            assertThatThrownBy(() -> service.syncFromTelegram(CHANNEL_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(CHANNEL_INACCESSIBLE);
        }
    }

    @Nested
    @DisplayName("syncFromTelegram — owner detection")
    class OwnerDetection {

        @Test
        @DisplayName("Should detect owner change when old owner differs from creator")
        void detectsOwnerChange() {
            setupTelegramResponses();
            when(dsl.select(CHANNELS.OWNER_ID)
                    .from(CHANNELS)
                    .where(any(org.jooq.Condition.class))
                    .fetchOne(CHANNELS.OWNER_ID))
                    .thenReturn(OLD_OWNER_ID);

            var result = service.syncFromTelegram(CHANNEL_ID);

            assertThat(result.ownerChanged()).isTrue();
            assertThat(result.oldOwnerId()).isEqualTo(OLD_OWNER_ID);
            assertThat(result.newOwnerId()).isEqualTo(CREATOR_ID);
        }

        @Test
        @DisplayName("Should report no change when owner is the same")
        void noChangeWhenSameOwner() {
            setupTelegramResponses();
            when(dsl.select(CHANNELS.OWNER_ID)
                    .from(CHANNELS)
                    .where(any(org.jooq.Condition.class))
                    .fetchOne(CHANNELS.OWNER_ID))
                    .thenReturn(CREATOR_ID);

            var result = service.syncFromTelegram(CHANNEL_ID);

            assertThat(result.ownerChanged()).isFalse();
            assertThat(result.newOwnerId()).isEqualTo(CREATOR_ID);
        }

        @Test
        @DisplayName("Should report no change for new channel (null old owner)")
        void noChangeForNewChannel() {
            setupTelegramResponses();
            when(dsl.select(CHANNELS.OWNER_ID)
                    .from(CHANNELS)
                    .where(any(org.jooq.Condition.class))
                    .fetchOne(CHANNELS.OWNER_ID))
                    .thenReturn(null);

            var result = service.syncFromTelegram(CHANNEL_ID);

            assertThat(result.ownerChanged()).isFalse();
            assertThat(result.oldOwnerId()).isNull();
            assertThat(result.newOwnerId()).isEqualTo(CREATOR_ID);
        }
    }

    @Nested
    @DisplayName("syncFromTelegram — user upsert")
    class UserUpsert {

        @Test
        @DisplayName("Should upsert users for all administrators")
        void upsertsAdminUsers() {
            setupTelegramResponses();
            when(dsl.select(CHANNELS.OWNER_ID)
                    .from(CHANNELS)
                    .where(any(org.jooq.Condition.class))
                    .fetchOne(CHANNELS.OWNER_ID))
                    .thenReturn(null);

            service.syncFromTelegram(CHANNEL_ID);

            verify(dsl).insertInto(USERS, USERS.ID, USERS.FIRST_NAME);
        }
    }

    @Nested
    @DisplayName("syncFromTelegram — Telegram API errors")
    class TelegramApiErrors {

        @Test
        @DisplayName("Should propagate exception when Telegram API fails on getChat")
        void propagatesGetChatError() {
            when(telegramChannelPort.getChat(CHANNEL_ID))
                    .thenThrow(new DomainException("SERVICE_UNAVAILABLE",
                            "Telegram API timeout"));

            assertThatThrownBy(() -> service.syncFromTelegram(CHANNEL_ID))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("Should propagate exception when Telegram API fails on getChatAdministrators")
        void propagatesAdminListError() {
            when(telegramChannelPort.getChat(CHANNEL_ID))
                    .thenReturn(chatInfo("channel"));
            when(telegramChannelPort.getChatAdministrators(CHANNEL_ID))
                    .thenThrow(new DomainException("SERVICE_UNAVAILABLE",
                            "Telegram API timeout"));

            assertThatThrownBy(() -> service.syncFromTelegram(CHANNEL_ID))
                    .isInstanceOf(DomainException.class);
        }
    }

    private void setupTelegramResponses() {
        when(telegramChannelPort.getChat(CHANNEL_ID))
                .thenReturn(chatInfo("channel"));
        when(telegramChannelPort.getChatAdministrators(CHANNEL_ID))
                .thenReturn(List.of(
                        creatorMember(CREATOR_ID),
                        adminMember(ADMIN_ID)));
        when(telegramChannelPort.getChatMemberCount(CHANNEL_ID))
                .thenReturn(MEMBER_COUNT);
    }

    private static ChatInfo chatInfo(String type) {
        return new ChatInfo(CHANNEL_ID, "Test Channel",
                "testchannel", type, "Test description");
    }

    private static ChatMemberInfo creatorMember(long userId) {
        return new ChatMemberInfo(userId, ChatMemberStatus.CREATOR,
                true, true, true, true);
    }

    private static ChatMemberInfo adminMember(long userId) {
        return new ChatMemberInfo(userId, ChatMemberStatus.ADMINISTRATOR,
                true, false, true, true);
    }
}
