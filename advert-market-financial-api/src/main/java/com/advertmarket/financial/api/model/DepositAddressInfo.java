package com.advertmarket.financial.api.model;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Generated deposit address with its subwallet identifier.
 *
 * @param depositAddress non-bounceable TON address (UQ... format)
 * @param subwalletId    unique subwallet ID for this deposit address
 */
public record DepositAddressInfo(
        @NonNull String depositAddress,
        long subwalletId) {
}
