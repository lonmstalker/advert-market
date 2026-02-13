package com.advertmarket.shared.event;

import com.advertmarket.shared.FenumGroup;
import org.checkerframework.checker.fenum.qual.Fenum;

/**
 * Kafka topic name constants.
 *
 * <p>Annotated with {@link Fenum @Fenum(FenumGroup.TOPIC_NAME)} for
 * static type-safety of topic name strings.
 */
@SuppressWarnings("fenum:assignment")
public final class TopicNames {

    /** Deal state change events. */
    public static final @Fenum(FenumGroup.TOPIC_NAME) String
            DEAL_STATE_CHANGED = "deal.state-changed";

    /** Deal deadline events. */
    public static final @Fenum(FenumGroup.TOPIC_NAME) String
            DEAL_DEADLINES = "deal.deadlines";

    /** Financial service commands. */
    public static final @Fenum(FenumGroup.TOPIC_NAME) String
            FINANCIAL_COMMANDS = "financial.commands";

    /** Financial service events. */
    public static final @Fenum(FenumGroup.TOPIC_NAME) String
            FINANCIAL_EVENTS = "financial.events";

    /** Delivery service commands. */
    public static final @Fenum(FenumGroup.TOPIC_NAME) String
            DELIVERY_COMMANDS = "delivery.commands";

    /** Delivery service events. */
    public static final @Fenum(FenumGroup.TOPIC_NAME) String
            DELIVERY_EVENTS = "delivery.events";

    /** Notification commands. */
    public static final @Fenum(FenumGroup.TOPIC_NAME) String
            COMMUNICATION_NOTIFICATIONS =
            "communication.notifications";

    /** Financial reconciliation events. */
    public static final @Fenum(FenumGroup.TOPIC_NAME) String
            FINANCIAL_RECONCILIATION = "financial.reconciliation";

    private TopicNames() {
    }
}
