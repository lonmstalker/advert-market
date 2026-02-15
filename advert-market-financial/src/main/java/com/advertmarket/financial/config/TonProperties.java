package com.advertmarket.financial.config;

import io.github.springpropertiesmd.api.annotation.PropertyDoc;
import io.github.springpropertiesmd.api.annotation.PropertyGroupDoc;
import io.github.springpropertiesmd.api.annotation.Requirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for TON blockchain integration.
 *
 * @param api     TON Center API settings
 * @param wallet  wallet mnemonic and allocation
 * @param deposit deposit polling settings
 * @param network blockchain network (testnet | mainnet)
 */
@ConfigurationProperties(prefix = "app.ton")
@PropertyGroupDoc(
        displayName = "TON Blockchain",
        description = "TON blockchain integration settings",
        category = "Financial"
)
@Validated
public record TonProperties(
        @PropertyDoc(
                description = "TON Center API settings",
                required = Requirement.REQUIRED
        )
        @Valid @NonNull Api api,

        @PropertyDoc(
                description = "Wallet configuration",
                required = Requirement.REQUIRED
        )
        @Valid @NonNull Wallet wallet,

        @PropertyDoc(
                description = "Deposit polling settings",
                required = Requirement.OPTIONAL
        )
        @Valid @DefaultValue Deposit deposit,

        @PropertyDoc(
                description = "Blockchain network: testnet or mainnet",
                required = Requirement.REQUIRED
        )
        @NotBlank @NonNull String network
) {

    /**
     * TON Center API settings.
     *
     * @param key       API key for TON Center
     * @param isTestnet whether to use testnet endpoint
     */
    public record Api(
            @PropertyDoc(
                    description = "TON Center API key",
                    required = Requirement.REQUIRED
            )
            @NotBlank @NonNull String key,

            @PropertyDoc(
                    description = "Use testnet API endpoint",
                    required = Requirement.OPTIONAL
            )
            @DefaultValue("true") boolean isTestnet
    ) {
    }

    /**
     * Wallet mnemonic and sequence allocation settings.
     *
     * @param mnemonic       space-separated BIP39 mnemonic words
     * @param allocationSize bulk sequence prefetch batch size
     */
    public record Wallet(
            @PropertyDoc(
                    description = "BIP39 mnemonic for master wallet",
                    required = Requirement.REQUIRED
            )
            @NotBlank @NonNull String mnemonic,

            @PropertyDoc(
                    description = "Subwallet sequence prefetch batch size",
                    required = Requirement.OPTIONAL
            )
            @Positive @DefaultValue("50") int allocationSize
    ) {
    }

    /**
     * Deposit polling settings (used by DepositWatcher, sub-session 2).
     *
     * @param pollInterval    interval between deposit polls
     * @param maxPollDuration maximum time to poll for a deposit
     * @param batchSize       number of pending deposits per poll batch
     */
    public record Deposit(
            @PropertyDoc(
                    description = "Interval between deposit polls",
                    required = Requirement.OPTIONAL
            )
            @DefaultValue("10s") Duration pollInterval,

            @PropertyDoc(
                    description = "Maximum polling duration for a deposit",
                    required = Requirement.OPTIONAL
            )
            @DefaultValue("30m") Duration maxPollDuration,

            @PropertyDoc(
                    description = "Number of pending deposits per poll batch",
                    required = Requirement.OPTIONAL
            )
            @Positive @DefaultValue("100") int batchSize
    ) {
    }
}