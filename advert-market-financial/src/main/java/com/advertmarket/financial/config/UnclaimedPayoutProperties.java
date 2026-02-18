package com.advertmarket.financial.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import java.time.Duration;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for unclaimed payout reminders and escalation.
 */
@ConfigurationProperties(prefix = "app.financial.unclaimed-payout")
@PropertyGroupDoc(
        displayName = "Unclaimed Payouts",
        description = "Scheduler settings for unclaimed payout reminders",
        category = "Financial"
)
public record UnclaimedPayoutProperties(

        @PropertyDoc(
                description = "Cron expression for unclaimed payout scheduler",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("0 30 2 * * *") @NonNull String cron,

        @PropertyDoc(
                description = "Max deals to process per scheduler run",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("100") int batchSize,

        @PropertyDoc(
                description = "Distributed lock TTL for scheduler run",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("10m") @NonNull Duration lockTtl,

        @PropertyDoc(
                description = "Reminder day thresholds after completion",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("1,7,21,30") @NonNull List<Integer> reminderDays,

        @PropertyDoc(
                description = "Day threshold for operator review escalation",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("30") int operatorReviewDay,

        @PropertyDoc(
                description = "Locale used for generated reminder notifications",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("ru") @NonNull String notificationLocale
) {
    /** Defensive copy for immutable list binding. */
    public UnclaimedPayoutProperties {
        reminderDays = List.copyOf(reminderDays);
    }
}
