package com.advertmarket.shared.financial;

import com.advertmarket.shared.model.Money;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Result of a commission calculation.
 *
 * @param commission platform commission amount
 * @param ownerPayout amount to be paid to the channel owner
 */
public record CommissionResult(
        @NonNull Money commission,
        @NonNull Money ownerPayout) {
}
