package com.advertmarket.marketplace.channel.service;

import static com.advertmarket.db.generated.tables.Channels.CHANNELS;

import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.marketplace.channel.config.ChannelStatisticsCollectorProperties;
import com.advertmarket.shared.error.ErrorCode;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodic collector for Telegram channel statistics.
 *
 * <p>For MVP, this collector refreshes subscriber_count and deactivates channels
 * that are no longer accessible in Telegram.
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(ChannelStatisticsCollectorProperties.class)
@Slf4j
public class ChannelStatisticsCollectorScheduler {

    private final DSLContext dsl;
    private final TelegramChannelPort telegramChannelPort;
    private final MetricsFacade metrics;
    private final ChannelStatisticsCollectorProperties properties;

    /**
     * Collects statistics for active channels in batches.
     */
    @Scheduled(
            fixedDelayString =
                    "${app.marketplace.channel.statistics.update-interval:6h}")
    @Transactional
    public void collectChannelStatistics() {
        if (!properties.enabled()) {
            return;
        }

        List<Long> channelIds = dsl.select(CHANNELS.ID)
                .from(CHANNELS)
                .where(CHANNELS.IS_ACTIVE.isTrue())
                .orderBy(CHANNELS.UPDATED_AT.asc())
                .limit(properties.batchSize())
                .fetch(CHANNELS.ID);

        if (channelIds.isEmpty()) {
            return;
        }

        for (Long channelId : channelIds) {
            collectChannelStatistics(channelId);
        }
    }

    private void collectChannelStatistics(long channelId) {
        try {
            int memberCount = telegramChannelPort.getChatMemberCount(channelId);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            dsl.update(CHANNELS)
                    .set(CHANNELS.SUBSCRIBER_COUNT, memberCount)
                    .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1))
                    .set(CHANNELS.UPDATED_AT, now)
                    .where(CHANNELS.ID.eq(channelId))
                    .execute();

            metrics.incrementCounter(MetricNames.CHANNEL_STATS_FETCHED,
                    "status", "success");
        } catch (DomainException ex) {
            handleDomainFailure(channelId, ex);
        } catch (RuntimeException ex) {
            metrics.incrementCounter(MetricNames.CHANNEL_STATS_FETCHED,
                    "status", "failed");
            log.warn("Channel stats sync failed for channel={}: {}",
                    channelId, ex.getMessage());
        }
    }

    private void handleDomainFailure(long channelId, DomainException ex) {
        ErrorCode errorCode = ErrorCode.resolve(ex.getErrorCode());
        metrics.incrementCounter(MetricNames.CHANNEL_STATS_FETCHED,
                "status", "failed");

        if (errorCode == ErrorCode.CHANNEL_NOT_FOUND
                || errorCode == ErrorCode.CHANNEL_BOT_NOT_MEMBER) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            dsl.update(CHANNELS)
                    .set(CHANNELS.IS_ACTIVE, false)
                    .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1))
                    .set(CHANNELS.UPDATED_AT, now)
                    .where(CHANNELS.ID.eq(channelId))
                    .execute();
            log.info("Channel {} deactivated after Telegram sync failure: {}",
                    channelId, errorCode);
            return;
        }

        log.warn("Channel stats sync domain failure for channel={} code={} msg={}",
                channelId, errorCode, ex.getMessage());
    }
}
