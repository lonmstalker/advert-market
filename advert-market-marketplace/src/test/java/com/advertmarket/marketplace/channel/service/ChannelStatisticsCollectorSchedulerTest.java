package com.advertmarket.marketplace.channel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.marketplace.channel.config.ChannelStatisticsCollectorProperties;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.metric.MetricsFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.OffsetDateTime;
import java.util.List;
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
                        true, 100, 0, 2);

        var scheduler = new TestableScheduler(
                telegramChannelPort, metrics, properties, List.of(101L));

        scheduler.collectChannelStatistics();

        verify(telegramChannelPort, times(3))
                .getChatMemberCount(101L);
        assertThat(scheduler.savedChannels()).isEmpty();
        assertThat(scheduler.deactivatedChannels()).isEmpty();
        assertThat(scheduler.retryBackoffs()).hasSize(2);
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

        TestableScheduler(
                TelegramChannelPort telegramChannelPort,
                MetricsFacade metrics,
                ChannelStatisticsCollectorProperties properties,
                List<Long> channelIds) {
            super(mock(DSLContext.class), telegramChannelPort,
                    metrics, properties);
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

        List<Long> savedChannels() {
            return savedChannels;
        }

        List<Long> deactivatedChannels() {
            return deactivatedChannels;
        }

        List<Long> retryBackoffs() {
            return retryBackoffs;
        }
    }
}
