package com.advertmarket.marketplace.channel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.api.notification.NotificationType;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberStatus;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.marketplace.channel.config.ChannelBotProperties;
import com.advertmarket.marketplace.channel.config.ChannelStatisticsCollectorProperties;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.outbox.OutboxRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChannelStatisticsCollectorScheduler")
class ChannelStatisticsCollectorSchedulerTest {

    @Test
    @DisplayName("Retries transient Telegram failures before giving up")
    void collectChannelStatistics_transientFailure_retries() {
        TelegramChannelPort telegramChannelPort = mock(TelegramChannelPort.class);
        when(telegramChannelPort.getChatMemberCount(101L))
                .thenThrow(new DomainException(
                        ErrorCodes.SERVICE_UNAVAILABLE,
                        "transient error"));

        MetricsFacade metrics = new MetricsFacade(new SimpleMeterRegistry());
        ChannelStatisticsCollectorProperties properties =
                new ChannelStatisticsCollectorProperties(
                        true, 100, 0, 2, Duration.ofHours(24));
        ChannelBotProperties botProperties = mock(ChannelBotProperties.class);
        when(botProperties.botUserId()).thenReturn(777L);

        var scheduler = new TestableScheduler(
                telegramChannelPort, botProperties, metrics, properties, List.of(101L));

        scheduler.collectChannelStatistics();

        verify(telegramChannelPort, times(3))
                .getChatMemberCount(101L);
        assertThat(scheduler.savedChannels()).isEmpty();
        assertThat(scheduler.deactivatedChannels()).isEmpty();
        assertThat(scheduler.retryBackoffs()).hasSize(2);
    }

    @Test
    @DisplayName("Deactivates channel and notifies owner when owner lost admin rights")
    void collectChannelStatistics_ownerRemoved_deactivatesAndNotifies() {
        TelegramChannelPort telegramChannelPort = mock(TelegramChannelPort.class);
        when(telegramChannelPort.getChatMemberCount(101L))
                .thenReturn(12_345);
        when(telegramChannelPort.getChatAdministrators(101L))
                .thenReturn(List.of(
                        admin(777L),
                        admin(999L)));

        MetricsFacade metrics = new MetricsFacade(new SimpleMeterRegistry());
        ChannelStatisticsCollectorProperties properties =
                new ChannelStatisticsCollectorProperties(
                        true, 100, 0, 2, Duration.ZERO);
        ChannelBotProperties botProperties = mock(ChannelBotProperties.class);
        when(botProperties.botUserId()).thenReturn(777L);

        var scheduler = new TestableScheduler(
                telegramChannelPort, botProperties, metrics, properties, List.of(101L));
        scheduler.setAdminContext(new ChannelStatisticsCollectorScheduler.AdminCheckContext(
                42L, "Test Channel", null));

        scheduler.collectChannelStatistics();

        assertThat(scheduler.deactivatedChannels()).contains(101L);
        assertThat(scheduler.notifications())
                .containsExactly(new NotificationCall(
                        42L,
                        NotificationType.CHANNEL_OWNERSHIP_LOST,
                        101L));
    }

    private static final class TestableScheduler
            extends ChannelStatisticsCollectorScheduler {

        private final List<Long> channelIds;
        private final java.util.List<Long> savedChannels =
                new java.util.ArrayList<>();
        private final java.util.List<Long> deactivatedChannels =
                new java.util.ArrayList<>();
        private final java.util.List<Long> retryBackoffs =
                new java.util.ArrayList<>();
        private final java.util.List<NotificationCall> notifications =
                new java.util.ArrayList<>();
        private ChannelStatisticsCollectorScheduler.AdminCheckContext adminContext;

        TestableScheduler(
                TelegramChannelPort telegramChannelPort,
                ChannelBotProperties botProperties,
                MetricsFacade metrics,
                ChannelStatisticsCollectorProperties properties,
                List<Long> channelIds) {
            super(mock(DSLContext.class), telegramChannelPort,
                    botProperties,
                    mock(OutboxRepository.class),
                    mock(JsonFacade.class),
                    metrics,
                    properties);
            this.channelIds = channelIds;
        }

        @Override
        List<Long> loadActiveChannelIds() {
            return channelIds;
        }

        @Override
        void saveSubscriberCount(long channelId, int memberCount,
                OffsetDateTime now) {
            savedChannels.add(channelId);
        }

        @Override
        void deactivateChannel(long channelId, OffsetDateTime now) {
            deactivatedChannels.add(channelId);
        }

        @Override
        boolean sleepBackoff(long backoffMs) {
            retryBackoffs.add(backoffMs);
            return true;
        }

        @Override
        Optional<ChannelStatisticsCollectorScheduler.AdminCheckContext> loadAdminCheckContext(
                long channelId) {
            return Optional.ofNullable(adminContext);
        }

        @Override
        void notifyOwner(long ownerId, NotificationType type,
                long channelId, String channelTitle) {
            notifications.add(new NotificationCall(ownerId, type, channelId));
        }

        @Override
        long countActiveDeals(long channelId) {
            return 0;
        }

        void setAdminContext(ChannelStatisticsCollectorScheduler.AdminCheckContext context) {
            this.adminContext = context;
        }

        List<Long> savedChannels() {
            return savedChannels;
        }

        List<Long> deactivatedChannels() {
            return deactivatedChannels;
        }

        List<Long> retryBackoffs() {
            return retryBackoffs;
        }

        List<NotificationCall> notifications() {
            return notifications;
        }
    }

    private static ChatMemberInfo admin(long userId) {
        return new ChatMemberInfo(
                userId,
                ChatMemberStatus.ADMINISTRATOR,
                true,
                true,
                true,
                true);
    }

    private record NotificationCall(
            long ownerId,
            NotificationType type,
            long channelId) {
    }
}
