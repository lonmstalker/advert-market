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

    // --- Account ---
    public static final @Fenum(FenumGroup.METRIC_NAME) String ACCOUNT_DELETED =
            "account.deleted";

    private MetricNames() {
    }
}
