package com.advertmarket.shared.event;

import com.advertmarket.shared.FenumGroup;
import org.checkerframework.checker.fenum.qual.Fenum;

/**
 * Event type discriminator constants for
 * {@link EventEnvelope#eventType()}.
 *
 * <p>Annotated with {@link Fenum @Fenum(FenumGroup.EVENT_TYPE)} for
 * static type-safety of event type strings.
 */
@SuppressWarnings("fenum:assignment")
public final class EventTypes {

    /** Deal state changed event. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            DEAL_STATE_CHANGED = "DEAL_STATE_CHANGED";

    /** Deadline set for a deal. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            DEADLINE_SET = "DEADLINE_SET";

    /** Command to watch for TON deposit. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            WATCH_DEPOSIT = "WATCH_DEPOSIT";

    /** Command to execute payout. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            EXECUTE_PAYOUT = "EXECUTE_PAYOUT";

    /** Command to execute refund. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            EXECUTE_REFUND = "EXECUTE_REFUND";

    /** Command to sweep commission. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            SWEEP_COMMISSION = "SWEEP_COMMISSION";

    /** Command to auto-refund late deposit. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            AUTO_REFUND_LATE_DEPOSIT =
            "AUTO_REFUND_LATE_DEPOSIT";

    /** Deposit confirmed on blockchain. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            DEPOSIT_CONFIRMED = "DEPOSIT_CONFIRMED";

    /** Deposit failed. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            DEPOSIT_FAILED = "DEPOSIT_FAILED";

    /** Command to publish post. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            PUBLISH_POST = "PUBLISH_POST";

    /** Command to verify delivery. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            VERIFY_DELIVERY = "VERIFY_DELIVERY";

    /** Delivery verification passed. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            DELIVERY_VERIFIED = "DELIVERY_VERIFIED";

    /** Delivery verification failed. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            DELIVERY_FAILED = "DELIVERY_FAILED";

    /** Send notification command. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            NOTIFICATION = "NOTIFICATION";

    /** Start reconciliation process. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            RECONCILIATION_START = "RECONCILIATION_START";

    /** Audit log event. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            AUDIT_EVENT = "AUDIT_EVENT";

    /** Payout confirmed on blockchain. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            PAYOUT_COMPLETED = "PAYOUT_COMPLETED";

    /** Payout deferred (no TON address). */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            PAYOUT_DEFERRED = "PAYOUT_DEFERRED";

    /** Payout permanently failed. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            PAYOUT_FAILED = "PAYOUT_FAILED";

    /** Refund confirmed on blockchain. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            REFUND_COMPLETED = "REFUND_COMPLETED";

    /** Refund deferred (no TON address). */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            REFUND_DEFERRED = "REFUND_DEFERRED";

    /** Refund permanently failed. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            REFUND_FAILED = "REFUND_FAILED";

    /** Withdrawal command from wallet. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            EXECUTE_WITHDRAWAL = "EXECUTE_WITHDRAWAL";

    /** Post publication result (success or failure). */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            PUBLICATION_RESULT = "PUBLICATION_RESULT";

    /** Reconciliation process completed. */
    public static final @Fenum(FenumGroup.EVENT_TYPE) String
            RECONCILIATION_RESULT = "RECONCILIATION_RESULT";

    private EventTypes() {
    }
}
