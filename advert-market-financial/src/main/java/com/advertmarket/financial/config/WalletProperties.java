package com.advertmarket.financial.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for user wallet service.
 */
@ConfigurationProperties(prefix = "app.wallet")
@PropertyGroupDoc(
        displayName = "Wallet",
        description = "User wallet configuration",
        category = "Financial"
)
public record WalletProperties(

        @PropertyDoc(
                description = "Minimum withdrawal amount in nanoTON",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("100000000") long minWithdrawalNano,

        @PropertyDoc(
                description = "Maximum cumulative auto-withdraw amount per 24h in nanoTON",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("1000000000000") long dailyVelocityLimitNano,

        @PropertyDoc(
                description = "Single withdrawal threshold that requires manual review, nanoTON",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("1000000000000") long manualApprovalThresholdNano
) {}
