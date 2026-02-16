package com.advertmarket.financial.ton.service;

import com.advertmarket.financial.config.TonProperties;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Tiered confirmation policy for deposit verification.
 *
 * <p>Pure function: evaluates the required number of block confirmations
 * based on the deposit amount and configured tiers.
 *
 * <p>NOT {@code @Component} — wired via
 * {@link com.advertmarket.financial.config.TonConfig}.
 */
public class ConfirmationPolicyService {

    private final List<TonProperties.Confirmation.Tier> tiers;

    /**
     * Creates the policy from the configured confirmation tiers.
     *
     * @param confirmation tier configuration
     */
    public ConfirmationPolicyService(TonProperties.@NonNull Confirmation confirmation) {
        this.tiers = confirmation.tiers();
    }

    /**
     * Determines the required confirmations for a given deposit amount.
     *
     * @param amountNano deposit amount in nanoTON
     * @return confirmation requirement (count + operator review flag)
     */
    public @NonNull ConfirmationRequirement requiredConfirmations(long amountNano) {
        for (var tier : tiers) {
            if (amountNano <= tier.thresholdNano()) {
                return new ConfirmationRequirement(
                        tier.confirmations(), tier.operatorReview());
            }
        }
        var last = tiers.getLast();
        return new ConfirmationRequirement(
                last.confirmations(), last.operatorReview());
    }
}
