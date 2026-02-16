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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    private static final String STATUS_TAG = "status";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";

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
    public void collectChannelStatistics() {
        if (!properties.enabled()) {
            return;
        }
        metrics.recordTimer(
                MetricNames.CHANNEL_STATS_COLLECTOR_CYCLE_DURATION,
                this::runCollectionCycle);
    }

    void runCollectionCycle() {
        List<Long> channelIds = loadActiveChannelIds();

        if (channelIds.isEmpty()) {
            return;
        }
        metrics.incrementCounter(
                MetricNames.CHANNEL_STATS_COLLECTOR_BATCH_SIZE,
                channelIds.size());

        for (Long channelId : channelIds) {
            collectChannelStatisticsForChannel(channelId);
        }
    }

    List<Long> loadActiveChannelIds() {
        return dsl.select(CHANNELS.ID)
                .from(CHANNELS)
                .where(CHANNELS.IS_ACTIVE.isTrue())
                .orderBy(CHANNELS.UPDATED_AT.asc())
                .limit(properties.batchSize())
                .fetch(CHANNELS.ID);
    }

    private void collectChannelStatisticsForChannel(long channelId) {
        int maxAttempts = properties.maxRetriesPerChannel() + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                int memberCount = telegramChannelPort
                        .getChatMemberCount(channelId);
                saveSubscriberCount(channelId, memberCount, nowUtc());
                markSuccess();
                return;
            } catch (DomainException ex) {
                ErrorCode errorCode = ErrorCode.resolve(ex.getErrorCode());
                if (errorCode == ErrorCode.CHANNEL_NOT_FOUND
                        || errorCode == ErrorCode.CHANNEL_BOT_NOT_MEMBER) {
                    deactivateChannel(channelId, nowUtc());
                    markFailure();
                    log.info("Channel {} deactivated after Telegram sync failure: {}",
                            channelId, errorCode);
                    return;
                }
                if (isTransient(errorCode) && attempt < maxAttempts) {
                    if (!retry(channelId, attempt, maxAttempts,
                            errorCode.name())) {
                        markFailure();
                        return;
                    }
                    continue;
                }
                markFailure();
                log.warn("Channel stats sync domain failure for channel={} code={} msg={}",
                        channelId, errorCode, ex.getMessage());
                return;
            }
        }
    }

    void saveSubscriberCount(long channelId, int memberCount,
            OffsetDateTime now) {
        dsl.update(CHANNELS)
                .set(CHANNELS.SUBSCRIBER_COUNT, memberCount)
                .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1))
                .set(CHANNELS.UPDATED_AT, now)
                .where(CHANNELS.ID.eq(channelId))
                .execute();
    }

    void deactivateChannel(long channelId, OffsetDateTime now) {
        dsl.update(CHANNELS)
                .set(CHANNELS.IS_ACTIVE, false)
                .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1))
                .set(CHANNELS.UPDATED_AT, now)
                .where(CHANNELS.ID.eq(channelId))
                .execute();
    }

    private boolean retry(long channelId, int attempt,
            int maxAttempts, String reason) {
        metrics.incrementCounter(
                MetricNames.CHANNEL_STATS_COLLECTOR_RETRY,
                "reason", reason);
        log.info("Retrying channel stats sync channel={} attempt={}/{} reason={}",
                channelId, attempt + 1, maxAttempts, reason);
        if (!sleepBackoff(properties.retryBackoffMs())) {
            log.warn("Retry backoff interrupted for channel={}",
                    channelId);
            return false;
        }
        return true;
    }

    boolean sleepBackoff(long backoffMs) {
        if (backoffMs <= 0) {
            return true;
        }
        if (Thread.currentThread().isInterrupted()) {
            return false;
        }
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(backoffMs));
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    private void markSuccess() {
        metrics.incrementCounter(
                MetricNames.CHANNEL_STATS_COLLECTOR_SUCCESS);
        metrics.incrementCounter(MetricNames.CHANNEL_STATS_FETCHED,
                STATUS_TAG, STATUS_SUCCESS);
    }

    private void markFailure() {
        metrics.incrementCounter(
                MetricNames.CHANNEL_STATS_COLLECTOR_FAILURE);
        metrics.incrementCounter(MetricNames.CHANNEL_STATS_FETCHED,
                STATUS_TAG, STATUS_FAILED);
    }

    private static boolean isTransient(ErrorCode errorCode) {
        return errorCode == ErrorCode.SERVICE_UNAVAILABLE
                || errorCode == ErrorCode.RATE_LIMIT_EXCEEDED;
    }

    private static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
