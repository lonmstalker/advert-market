package com.advertmarket.shared.logging;

import com.advertmarket.shared.model.DealId;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.MDC;

/**
 * MDC key constants and utility methods for structured logging.
 */
public final class MdcKeys {

    /** Correlation ID propagated across HTTP and Kafka boundaries. */
    public static final String CORRELATION_ID = "correlationId";

    /** Authenticated user identifier. */
    public static final String USER_ID = "userId";

    /** Deal identifier for deal-scoped operations. */
    public static final String DEAL_ID = "dealId";

    private MdcKeys() {
    }

    /** Puts the user ID into MDC. */
    public static void putUserId(long userId) {
        MDC.put(USER_ID, String.valueOf(userId));
    }

    /** Removes the user ID from MDC. */
    public static void clearUserId() {
        MDC.remove(USER_ID);
    }

    /** Puts the deal ID into MDC. */
    public static void putDealId(@NonNull DealId dealId) {
        MDC.put(DEAL_ID, dealId.toString());
    }

    /** Removes the deal ID from MDC. */
    public static void clearDealId() {
        MDC.remove(DEAL_ID);
    }
}
