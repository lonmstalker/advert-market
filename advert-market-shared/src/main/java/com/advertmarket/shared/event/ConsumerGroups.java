package com.advertmarket.shared.event;

import com.advertmarket.shared.FenumGroup;
import org.checkerframework.checker.fenum.qual.Fenum;

/**
 * Kafka consumer group ID constants.
 *
 * <p>Annotated with {@link Fenum @Fenum(FenumGroup.CONSUMER_GROUP)} for
 * static type-safety of consumer group strings.
 */
@SuppressWarnings("fenum:assignment")
public final class ConsumerGroups {

    /** Processes financial commands (deposit, payout, refund). */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            FINANCIAL_COMMAND_HANDLER =
            "financial-command-handler";

    /** Processes delivery commands (publish, verify). */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            DELIVERY_COMMAND_HANDLER =
            "delivery-command-handler";

    /** Sends Telegram notifications. */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            NOTIFICATION_SENDER = "notification-sender";

    /** Maintains deal state CQRS projection. */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            DEAL_STATE_PROJECTION = "deal-state-projection";

    /** Schedules and manages deal deadlines. */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            DEAL_DEADLINE_SCHEDULER =
            "deal-deadline-scheduler";

    /** Watches TON blockchain for deposits. */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            TON_DEPOSIT_WATCHER = "ton-deposit-watcher";

    /** Executes TON payouts. */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            TON_PAYOUT_EXECUTOR = "ton-payout-executor";

    /** Executes TON refunds. */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            TON_REFUND_EXECUTOR = "ton-refund-executor";

    /** Handles reconciliation events. */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            RECONCILIATION_HANDLER = "reconciliation-handler";

    /** Collects analytics events. */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            ANALYTICS_COLLECTOR = "analytics-collector";

    /** Processes dead-letter records. */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            DEAD_LETTER_PROCESSOR = "dead-letter-processor";

    /** Handles financial result events (deposit, payout, refund). */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            FINANCIAL_EVENT_HANDLER =
            "financial-event-handler";

    /** Handles delivery result events (publication, verification). */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            DELIVERY_EVENT_HANDLER =
            "delivery-event-handler";

    /** Executes deal post-transition workflow side-effects. */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            DEAL_WORKFLOW_ENGINE =
            "deal-workflow-engine";

    /** Handles reconciliation result events. */
    public static final @Fenum(FenumGroup.CONSUMER_GROUP) String
            RECONCILIATION_RESULT_HANDLER =
            "reconciliation-result-handler";

    private ConsumerGroups() {
    }
}
