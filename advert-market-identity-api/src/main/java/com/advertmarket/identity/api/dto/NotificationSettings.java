package com.advertmarket.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * User notification preferences.
 *
 * @param deals     deal-related notifications
 * @param financial financial notifications
 * @param disputes  dispute notifications
 */
@Schema(description = "Notification settings")
public record NotificationSettings(
        @Schema(description = "Deal notifications")
        @NonNull DealNotifications deals,
        @Schema(description = "Financial notifications")
        @NonNull FinancialNotifications financial,
        @Schema(description = "Dispute notifications")
        @NonNull DisputeNotifications disputes
) {

    /**
     * Deal-related notification preferences.
     */
    @Schema(description = "Deal notification settings")
    public record DealNotifications(
            boolean newOffers,
            boolean acceptReject,
            boolean deliveryStatus
    ) {}

    /**
     * Financial notification preferences.
     */
    @Schema(description = "Financial notification settings")
    public record FinancialNotifications(
            boolean deposits,
            boolean payouts,
            boolean escrow
    ) {}

    /**
     * Dispute notification preferences.
     */
    @Schema(description = "Dispute notification settings")
    public record DisputeNotifications(
            boolean opened,
            boolean resolved
    ) {}

    /**
     * Returns default notification settings with all notifications enabled.
     */
    public static NotificationSettings defaults() {
        return new NotificationSettings(
                new DealNotifications(true, true, true),
                new FinancialNotifications(true, true, true),
                new DisputeNotifications(true, true));
    }
}
