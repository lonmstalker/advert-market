package com.advertmarket.shared.exception;

import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.Money;
import java.util.Map;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Thrown when an account has insufficient balance.
 */
@Getter
public class InsufficientBalanceException
        extends DomainException {

    private final @NonNull AccountId accountId;
    private final @NonNull Money requested;
    private final @NonNull Money available;

    /**
     * Creates an insufficient-balance exception.
     *
     * @param accountId the account with insufficient balance
     * @param requested the requested amount
     * @param available the available balance
     */
    public InsufficientBalanceException(
            @NonNull AccountId accountId,
            @NonNull Money requested,
            @NonNull Money available) {
        super("INSUFFICIENT_BALANCE",
                String.format(
                        "Insufficient balance on %s:"
                                + " requested %s,"
                                + " available %s",
                        accountId.value(),
                        requested, available),
                Map.of("accountId", accountId.value(),
                        "requested", requested.nanoTon(),
                        "available", available.nanoTon()));
        this.accountId = accountId;
        this.requested = requested;
        this.available = available;
    }
}
