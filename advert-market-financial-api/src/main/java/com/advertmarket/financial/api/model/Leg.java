package com.advertmarket.financial.api.model;

import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * One side of a double-entry ledger transfer.
 *
 * @param accountId target account
 * @param entryType business operation type
 * @param amount positive monetary amount
 * @param side debit or credit
 */
public record Leg(
        @NonNull AccountId accountId,
        @NonNull EntryType entryType,
        @NonNull Money amount,
        @NonNull Side side) {

    /** Direction of the ledger leg: debit (money out) or credit (money in). */
    public enum Side { DEBIT, CREDIT }

    /** Validates non-null parameters and rejects zero amount. */
    public Leg {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(entryType, "entryType");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(side, "side");
        if (amount.isZero()) {
            throw new IllegalArgumentException(
                    "Leg amount must be positive");
        }
    }

    /** Returns the nanoTON value for the debit column (positive if DEBIT, 0 if CREDIT). */
    public long debitNano() {
        return side == Side.DEBIT ? amount.nanoTon() : 0;
    }

    /** Returns the nanoTON value for the credit column (positive if CREDIT, 0 if DEBIT). */
    public long creditNano() {
        return side == Side.CREDIT ? amount.nanoTon() : 0;
    }

    /** Returns {@code true} if this is a debit leg. */
    public boolean isDebit() {
        return side == Side.DEBIT;
    }

    /** Returns {@code true} if this is a credit leg. */
    public boolean isCredit() {
        return side == Side.CREDIT;
    }
}
