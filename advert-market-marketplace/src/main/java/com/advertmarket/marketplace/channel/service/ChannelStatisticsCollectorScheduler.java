package com.advertmarket.marketplace.channel.service;

import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.Deals.DEALS;

import com.advertmarket.communication.api.event.NotificationEvent;
import com.advertmarket.communication.api.notification.NotificationType;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberInfo;
import com.advertmarket.marketplace.api.dto.telegram.ChatMemberStatus;
import com.advertmarket.marketplace.api.port.TelegramChannelPort;
import com.advertmarket.marketplace.channel.config.ChannelBotProperties;
import com.advertmarket.marketplace.channel.config.ChannelStatisticsCollectorProperties;
import com.advertmarket.shared.error.ErrorCode;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.outbox.OutboxStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
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
@SuppressWarnings({"fenum:argument", "fenum:assignment"})
public class ChannelStatisticsCollectorScheduler {

    private static final String STATUS_TAG = "status";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";
    private static final String ADMIN_RESULT_TAG = "result";
    private static final String ADMIN_RESULT_PASS = "PASS";
    private static final String ADMIN_RESULT_FAIL_OWNER_REMOVED = "FAIL_OWNER_REMOVED";
    private static final String ADMIN_RESULT_FAIL_BOT_REMOVED = "FAIL_BOT_REMOVED";
    private static final int BASIS_POINTS = 10_000;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO_RATE = BigDecimal.ZERO.setScale(
            2, RoundingMode.HALF_UP);

    private final DSLContext dsl;
    private final TelegramChannelPort telegramChannelPort;
    private final ChannelBotProperties botProperties;
    private final OutboxRepository outboxRepository;
    private final JsonFacade jsonFacade;
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
                .orderBy(CHANNELS.STATS_UPDATED_AT.asc().nullsFirst())
                .limit(properties.batchSize())
                .fetch(CHANNELS.ID);
    }

    private void collectChannelStatisticsForChannel(long channelId) {
        int maxAttempts = properties.maxRetriesPerChannel() + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                int memberCount = telegramChannelPort
                        .getChatMemberCount(channelId);
                ChannelStatsSnapshot snapshot = buildStatsSnapshot(memberCount);
                OffsetDateTime now = nowUtc();
                saveSubscriberCount(channelId, memberCount, snapshot.avgViews(),
                        snapshot.engagementRate(), now);
                checkAdminState(channelId, now);
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

    void saveSubscriberCount(
            long channelId,
            int memberCount,
            int avgViews,
            BigDecimal engagementRate,
            OffsetDateTime now) {
        dsl.update(CHANNELS)
                .set(CHANNELS.SUBSCRIBER_COUNT, memberCount)
                .set(CHANNELS.AVG_VIEWS, avgViews)
                .set(CHANNELS.ENGAGEMENT_RATE, engagementRate)
                .set(CHANNELS.STATS_UPDATED_AT, now)
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

    void markBotVerifiedAt(long channelId, OffsetDateTime now) {
        dsl.update(CHANNELS)
                .set(CHANNELS.BOT_VERIFIED_AT, now)
                .set(CHANNELS.VERSION, CHANNELS.VERSION.plus(1))
                .set(CHANNELS.UPDATED_AT, now)
                .where(CHANNELS.ID.eq(channelId))
                .execute();
    }

    void notifyOwner(long ownerId, NotificationType type,
            long channelId, @Nullable String channelTitle) {
        var payload = new NotificationEvent(
                ownerId,
                type.name(),
                "ru",
                Map.of(
                        "channel_name",
                        channelTitle == null || channelTitle.isBlank()
                                ? String.valueOf(channelId)
                                : channelTitle),
                null);
        var envelope = EventEnvelope.create(
                EventTypes.NOTIFICATION,
                null,
                payload);
        outboxRepository.save(OutboxEntry.builder()
                .dealId(null)
                .idempotencyKey(
                        "channel-admin-check:%d:%s:%d"
                                .formatted(channelId, type.name(), ownerId))
                .topic(TopicNames.COMMUNICATION_NOTIFICATIONS)
                .partitionKey(String.valueOf(ownerId))
                .payload(jsonFacade.toJson(envelope))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(0)
                .createdAt(Instant.now())
                .build());
    }

    void checkAdminState(long channelId, OffsetDateTime now) {
        var context = loadAdminCheckContext(channelId).orElse(null);
        if (context == null || !isAdminCheckDue(context.botVerifiedAt(), now)) {
            return;
        }
        var admins = telegramChannelPort.getChatAdministrators(channelId);
        if (!isOwnerStillAdmin(admins, context.ownerId())) {
            handleOwnerRemoved(channelId, now, context);
            return;
        }
        if (!isBotStillAdmin(admins)) {
            handleBotRemoved(channelId, now, context, admins);
            return;
        }
        markBotVerifiedAt(channelId, now);
        metrics.incrementCounter(
                MetricNames.CHANNEL_ADMIN_CHECK,
                ADMIN_RESULT_TAG, ADMIN_RESULT_PASS);
    }

    Optional<AdminCheckContext> loadAdminCheckContext(long channelId) {
        return dsl.select(
                        CHANNELS.OWNER_ID,
                        CHANNELS.TITLE,
                        CHANNELS.BOT_VERIFIED_AT)
                .from(CHANNELS)
                .where(CHANNELS.ID.eq(channelId))
                .fetchOptional(record -> new AdminCheckContext(
                        record.get(CHANNELS.OWNER_ID),
                        record.get(CHANNELS.TITLE),
                        record.get(CHANNELS.BOT_VERIFIED_AT)));
    }

    long countActiveDeals(long channelId) {
        return dsl.fetchCount(
                dsl.selectFrom(DEALS)
                        .where(DEALS.CHANNEL_ID.eq(channelId))
                        .and(DEALS.STATUS.notIn(
                                "COMPLETED_RELEASED",
                                "CANCELLED",
                                "REFUNDED",
                                "PARTIALLY_REFUNDED",
                                "EXPIRED")));
    }

    private void handleOwnerRemoved(
            long channelId,
            OffsetDateTime now,
            AdminCheckContext context) {
        deactivateChannel(channelId, now);
        notifyOwner(
                context.ownerId(),
                NotificationType.CHANNEL_OWNERSHIP_LOST,
                channelId,
                context.title());
        metrics.incrementCounter(
                MetricNames.CHANNEL_ADMIN_CHECK,
                ADMIN_RESULT_TAG, ADMIN_RESULT_FAIL_OWNER_REMOVED);
        log.warn(
                "Channel {} deactivated: owner {} is no longer admin",
                channelId,
                context.ownerId());
        alertIfActiveDeals(channelId);
    }

    private void handleBotRemoved(
            long channelId,
            OffsetDateTime now,
            AdminCheckContext context,
            List<ChatMemberInfo> admins) {
        deactivateChannel(channelId, now);
        var type = containsBot(admins)
                ? NotificationType.CHANNEL_BOT_DEMOTED
                : NotificationType.CHANNEL_BOT_REMOVED;
        notifyOwner(context.ownerId(), type, channelId, context.title());
        metrics.incrementCounter(
                MetricNames.CHANNEL_ADMIN_CHECK,
                ADMIN_RESULT_TAG, ADMIN_RESULT_FAIL_BOT_REMOVED);
        log.warn("Channel {} deactivated: bot admin check failed", channelId);
        alertIfActiveDeals(channelId);
    }

    private void alertIfActiveDeals(long channelId) {
        long activeDeals = countActiveDeals(channelId);
        if (activeDeals > 0) {
            log.error(
                    "Channel {} lost admin safety with {} active deals."
                            + " Operator intervention required.",
                    channelId,
                    activeDeals);
        }
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

    private ChannelStatsSnapshot buildStatsSnapshot(int memberCount) {
        if (memberCount <= 0) {
            return new ChannelStatsSnapshot(0, ZERO_RATE);
        }
        int avgViews = calculateAvgViews(memberCount);
        BigDecimal engagementRate = BigDecimal.valueOf(avgViews)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(memberCount),
                        2, RoundingMode.HALF_UP);
        return new ChannelStatsSnapshot(avgViews, engagementRate);
    }

    private int calculateAvgViews(int memberCount) {
        long estimatedViews = ((long) memberCount
                * properties.estimatedViewRateBp()) / BASIS_POINTS;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, estimatedViews));
    }

    private boolean isAdminCheckDue(
            @Nullable OffsetDateTime lastVerifiedAt,
            OffsetDateTime now) {
        if (lastVerifiedAt == null) {
            return true;
        }
        return lastVerifiedAt.plus(properties.adminCheckInterval())
                .isBefore(now);
    }

    private static boolean isOwnerStillAdmin(
            List<ChatMemberInfo> admins,
            long ownerId) {
        return admins.stream()
                .anyMatch(member -> member.userId() == ownerId
                        && isAdminStatus(member.status()));
    }

    private boolean isBotStillAdmin(List<ChatMemberInfo> admins) {
        return admins.stream()
                .filter(member -> member.userId() == botProperties.botUserId())
                .anyMatch(member ->
                        isAdminStatus(member.status())
                                && member.canPostMessages());
    }

    private boolean containsBot(List<ChatMemberInfo> admins) {
        return admins.stream()
                .anyMatch(member -> member.userId() == botProperties.botUserId());
    }

    private static boolean isAdminStatus(ChatMemberStatus status) {
        return status == ChatMemberStatus.CREATOR
                || status == ChatMemberStatus.ADMINISTRATOR;
    }

    private static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    record AdminCheckContext(
            long ownerId,
            @Nullable String title,
            @Nullable OffsetDateTime botVerifiedAt) {
    }

    private record ChannelStatsSnapshot(
            int avgViews,
            BigDecimal engagementRate) {
    }
}
