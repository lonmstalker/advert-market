package com.advertmarket.financial.api.model;

import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Read model for a persisted ledger entry.
 *
 * <p>Each entry has exactly one of debitNano or creditNano &gt; 0.
 *
 * @param id database identity
 * @param dealId optional deal reference
 * @param accountId target account
 * @param entryType business operation type
 * @param debitNano debit amount in nanoTON (0 if credit entry)
 * @param creditNano credit amount in nanoTON (0 if debit entry)
 * @param idempotencyKey exactly-once key
 * @param txRef transaction reference grouping related legs
 * @param description optional human-readable description
 * @param createdAt creation timestamp
 */
public record LedgerEntry(
        long id,
        @Nullable DealId dealId,
        @NonNull AccountId accountId,
        @NonNull EntryType entryType,
        long debitNano,
        long creditNano,
        @NonNull String idempotencyKey,
        @NonNull UUID txRef,
        @Nullable String description,
        @NonNull Instant createdAt) {

    /** Validates invariants: exactly one of debitNano/creditNano must be positive. */
    public LedgerEntry {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(entryType, "entryType");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(txRef, "txRef");
        Objects.requireNonNull(createdAt, "createdAt");
        if (debitNano < 0 || creditNano < 0) {
            throw new IllegalArgumentException(
                    "debitNano and creditNano must be >= 0");
        }
        if ((debitNano == 0) == (creditNano == 0)) {
            throw new IllegalArgumentException(
                    "Exactly one of debitNano or creditNano must be > 0");
        }
    }
}
