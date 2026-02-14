package com.advertmarket.shared.metric;

import com.advertmarket.shared.FenumGroup;
import org.checkerframework.checker.fenum.qual.Fenum;

/**
 * Centralized metric name constants for {@link MetricsFacade}.
 */
@SuppressWarnings("fenum:assignment")
public final class MetricNames {

    // --- Auth ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String AUTH_LOGIN_SUCCESS =
            "auth.login.success";
    public static final @Fenum(FenumGroup.METRIC_NAME) String AUTH_LOGOUT =
            "auth.logout";
    public static final @Fenum(FenumGroup.METRIC_NAME) String AUTH_BLOCKED_ACCESS =
            "auth.blocked.access";
    public static final @Fenum(FenumGroup.METRIC_NAME) String AUTH_RATE_LIMITER_REDIS_ERROR =
            "auth.rate_limiter.redis_error";
    public static final @Fenum(FenumGroup.METRIC_NAME) String AUTH_ACCESS_DENIED =
            "auth.access.denied";

    // --- Account ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String ACCOUNT_DELETED =
            "account.deleted";

    // --- Deal ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String DEAL_CREATED =
            "deal.created";
    public static final @Fenum(FenumGroup.METRIC_NAME) String DEAL_ACCEPTED =
            "deal.accepted";
    public static final @Fenum(FenumGroup.METRIC_NAME) String DEAL_CANCELLED =
            "deal.cancelled";
    public static final @Fenum(FenumGroup.METRIC_NAME) String DEAL_COMPLETED =
            "deal.completed";
    public static final @Fenum(FenumGroup.METRIC_NAME) String DEAL_DISPUTED =
            "deal.disputed";
    public static final @Fenum(FenumGroup.METRIC_NAME) String DEAL_STATE_TRANSITION =
            "deal.state.transition";

    // --- Financial ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String DEPOSIT_RECEIVED =
            "deposit.received";
    public static final @Fenum(FenumGroup.METRIC_NAME) String DEPOSIT_TIMEOUT =
            "deposit.timeout";
    public static final @Fenum(FenumGroup.METRIC_NAME) String PAYOUT_COMPLETED =
            "payout.completed";
    public static final @Fenum(FenumGroup.METRIC_NAME) String PAYOUT_FAILED =
            "payout.failed";
    public static final @Fenum(FenumGroup.METRIC_NAME) String REFUND_COMPLETED =
            "refund.completed";
    public static final @Fenum(FenumGroup.METRIC_NAME) String LEDGER_ENTRY_CREATED =
            "ledger.entry.created";
    public static final @Fenum(FenumGroup.METRIC_NAME) String COMMISSION_CALCULATED =
            "commission.calculated";

    // --- Channel ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String CHANNEL_REGISTERED =
            "channel.registered";
    public static final @Fenum(FenumGroup.METRIC_NAME) String CHANNEL_STATS_FETCHED =
            "channel.stats.fetched";
    public static final @Fenum(FenumGroup.METRIC_NAME) String CHANNEL_API_CALL =
            "channel.api.call";
    public static final @Fenum(FenumGroup.METRIC_NAME) String CHANNEL_CACHE_HIT =
            "channel.cache.hit";
    public static final @Fenum(FenumGroup.METRIC_NAME) String CHANNEL_CACHE_MISS =
            "channel.cache.miss";
    public static final @Fenum(FenumGroup.METRIC_NAME) String CHANNEL_CACHE_STALE =
            "channel.cache.stale";
    public static final @Fenum(FenumGroup.METRIC_NAME) String CHANNEL_BOT_STATUS_CHANGE =
            "channel.bot.status.change";
    public static final @Fenum(FenumGroup.METRIC_NAME) String CHANNEL_DEACTIVATED_TOTAL =
            "channel.deactivated.total";

    // --- Delivery ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String DELIVERY_VERIFIED =
            "delivery.verified";
    public static final @Fenum(FenumGroup.METRIC_NAME) String DELIVERY_FAILED =
            "delivery.failed";

    // --- Canary ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String CANARY_PERCENT_CURRENT =
            "canary.percent.current";
    public static final @Fenum(FenumGroup.METRIC_NAME) String CANARY_ROUTE_DECISION =
            "canary.route.decision";

    // --- Telegram Webhook ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String WEBHOOK_LATENCY =
            "telegram.webhook.latency";
    public static final @Fenum(FenumGroup.METRIC_NAME) String DEDUP_ACQUIRED =
            "telegram.update.dedup.acquired";
    public static final @Fenum(FenumGroup.METRIC_NAME) String DEDUP_DUPLICATE =
            "telegram.update.duplicates";

    // --- Reconciliation ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String RECONCILIATION_COMPLETED =
            "reconciliation.completed";
    public static final @Fenum(FenumGroup.METRIC_NAME) String RECONCILIATION_FAILED =
            "reconciliation.failed";

    // --- Publication ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String PUBLICATION_SUCCEEDED =
            "publication.succeeded";
    public static final @Fenum(FenumGroup.METRIC_NAME) String PUBLICATION_FAILED =
            "publication.failed";

    // --- Refund ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String REFUND_FAILED =
            "refund.failed";

    // --- Worker Events ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String WORKER_EVENT_RECEIVED =
            "worker.event.received";
    public static final @Fenum(FenumGroup.METRIC_NAME) String WORKER_EVENT_DUPLICATE =
            "worker.event.duplicate";
    public static final @Fenum(FenumGroup.METRIC_NAME) String WORKER_CALLBACK_HTTP_RECEIVED =
            "worker.callback.http.received";
    public static final @Fenum(FenumGroup.METRIC_NAME) String INTERNAL_AUTH_FAILED =
            "internal.auth.failed";

    // --- Errors ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String ERRORS_DOMAIN =
            "errors.domain";
    public static final @Fenum(FenumGroup.METRIC_NAME) String ERRORS_UNHANDLED =
            "errors.unhandled";

    // --- System ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String OUTBOX_PUBLISHED =
            "outbox.published";
    public static final @Fenum(FenumGroup.METRIC_NAME) String OUTBOX_POLL_COUNT =
            "outbox.poll.count";
    public static final @Fenum(FenumGroup.METRIC_NAME) String OUTBOX_RECORDS_FAILED =
            "outbox.records.failed";
    public static final @Fenum(FenumGroup.METRIC_NAME) String OUTBOX_LAG =
            "outbox.lag";
    public static final @Fenum(FenumGroup.METRIC_NAME) String LOCK_ACQUIRED =
            "lock.acquired";
    public static final @Fenum(FenumGroup.METRIC_NAME) String LOCK_TIMEOUT =
            "lock.timeout";

    private MetricNames() {
    }
}
