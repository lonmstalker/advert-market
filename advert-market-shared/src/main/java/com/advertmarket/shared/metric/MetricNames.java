package com.advertmarket.shared.metric;

import com.advertmarket.shared.FenumGroup;
import org.checkerframework.checker.fenum.qual.Fenum;

/**
 * Centralized metric name constants for {@link MetricsFacade}.
 */
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

    // --- Delivery ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String DELIVERY_VERIFIED =
            "delivery.verified";
    public static final @Fenum(FenumGroup.METRIC_NAME) String DELIVERY_FAILED =
            "delivery.failed";

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
