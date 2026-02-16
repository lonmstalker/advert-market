package com.advertmarket.app.config;

import com.advertmarket.communication.api.event.NotificationEvent;
import com.advertmarket.deal.api.event.DeadlineSetEvent;
import com.advertmarket.deal.api.event.DealStateChangedEvent;
import com.advertmarket.delivery.api.event.DeliveryFailedEvent;
import com.advertmarket.delivery.api.event.DeliveryVerifiedEvent;
import com.advertmarket.delivery.api.event.PublicationResultEvent;
import com.advertmarket.delivery.api.event.PublishPostCommand;
import com.advertmarket.delivery.api.event.VerifyDeliveryCommand;
import com.advertmarket.financial.api.event.AutoRefundLateDepositCommand;
import com.advertmarket.financial.api.event.DepositConfirmedEvent;
import com.advertmarket.financial.api.event.DepositFailedEvent;
import com.advertmarket.financial.api.event.ExecutePayoutCommand;
import com.advertmarket.financial.api.event.ExecuteRefundCommand;
import com.advertmarket.financial.api.event.PayoutCompletedEvent;
import com.advertmarket.financial.api.event.PayoutDeferredEvent;
import com.advertmarket.financial.api.event.ReconciliationResultEvent;
import com.advertmarket.financial.api.event.ReconciliationStartEvent;
import com.advertmarket.financial.api.event.RefundCompletedEvent;
import com.advertmarket.financial.api.event.RefundDeferredEvent;
import com.advertmarket.financial.api.event.SweepCommissionCommand;
import com.advertmarket.financial.api.event.WatchDepositCommand;
import com.advertmarket.shared.event.EventTypeRegistry;
import com.advertmarket.shared.event.EventTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all event types in the {@link EventTypeRegistry}.
 */
@Configuration
public class KafkaEventTypeConfig {

    /** Creates and populates the event type registry. */
    @Bean
    public EventTypeRegistry eventTypeRegistry() {
        var registry = new EventTypeRegistry();

        registry.register(EventTypes.DEAL_STATE_CHANGED,
                DealStateChangedEvent.class);
        registry.register(EventTypes.DEADLINE_SET,
                DeadlineSetEvent.class);

        registry.register(EventTypes.WATCH_DEPOSIT,
                WatchDepositCommand.class);
        registry.register(EventTypes.EXECUTE_PAYOUT,
                ExecutePayoutCommand.class);
        registry.register(EventTypes.EXECUTE_REFUND,
                ExecuteRefundCommand.class);
        registry.register(EventTypes.SWEEP_COMMISSION,
                SweepCommissionCommand.class);
        registry.register(EventTypes.AUTO_REFUND_LATE_DEPOSIT,
                AutoRefundLateDepositCommand.class);
        registry.register(EventTypes.DEPOSIT_CONFIRMED,
                DepositConfirmedEvent.class);
        registry.register(EventTypes.DEPOSIT_FAILED,
                DepositFailedEvent.class);

        registry.register(EventTypes.PUBLISH_POST,
                PublishPostCommand.class);
        registry.register(EventTypes.VERIFY_DELIVERY,
                VerifyDeliveryCommand.class);
        registry.register(EventTypes.DELIVERY_VERIFIED,
                DeliveryVerifiedEvent.class);
        registry.register(EventTypes.DELIVERY_FAILED,
                DeliveryFailedEvent.class);

        registry.register(EventTypes.NOTIFICATION,
                NotificationEvent.class);
        registry.register(EventTypes.RECONCILIATION_START,
                ReconciliationStartEvent.class);

        registry.register(EventTypes.PAYOUT_COMPLETED,
                PayoutCompletedEvent.class);
        registry.register(EventTypes.PAYOUT_DEFERRED,
                PayoutDeferredEvent.class);
        registry.register(EventTypes.REFUND_COMPLETED,
                RefundCompletedEvent.class);
        registry.register(EventTypes.REFUND_DEFERRED,
                RefundDeferredEvent.class);
        registry.register(EventTypes.PUBLICATION_RESULT,
                PublicationResultEvent.class);
        registry.register(EventTypes.RECONCILIATION_RESULT,
                ReconciliationResultEvent.class);

        return registry;
    }
}
