package com.advertmarket.financial.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for runtime TON network fee accounting.
 */
@ConfigurationProperties(prefix = "app.financial.network-fee")
@PropertyGroupDoc(
        displayName = "Financial Network Fee",
        description = "Default network fee estimate for outbound TON transfers",
        category = "Financial"
)
public record NetworkFeeProperties(

        @PropertyDoc(
                description = "Default fee estimate in nanoTON "
                        + "for payout/refund/sweep runtime ledger entries",
                required = Requirement.OPTIONAL
        )
        @DefaultValue("5000000") long defaultEstimateNano
) {
}
