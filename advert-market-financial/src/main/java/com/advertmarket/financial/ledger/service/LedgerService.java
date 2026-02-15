package com.advertmarket.financial.ledger.service;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.BalanceCachePort;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.ledger.repository.JooqAccountBalanceRepository;
import com.advertmarket.financial.ledger.repository.JooqLedgerRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.pagination.CursorPage;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Core double-entry ledger service.
 *
 * <p>Records balanced transfers with idempotency, deadlock prevention,
 * and post-commit cache updates.
 */
@Service
@RequiredArgsConstructor
public class LedgerService implements LedgerPort {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final JooqLedgerRepository ledgerRepository;
    private final JooqAccountBalanceRepository balanceRepository;
    private final BalanceCachePort balanceCache;
    private final MetricsFacade metricsFacade;

    @Override
    @Transactional
    public @NonNull UUID transfer(@NonNull TransferRequest request) {
        List<Leg> legs = request.legs();

        // 1. Idempotency: INSERT-first, atomic
        String idempotencyKey = request.idempotencyKey().value();
        if (!ledgerRepository.tryInsertIdempotencyKey(idempotencyKey)) {
            return ledgerRepository.findTxRefByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new DomainException(
                            ErrorCodes.LEDGER_INCONSISTENCY,
                            "Idempotency key exists but no entries found: "
                                    + idempotencyKey));
        }

        // 2. Validate balance: SUM(debit) == SUM(credit)
        validateBalance(legs);

        // 3. Sort ALL legs by accountId for deadlock prevention
        List<Leg> sortedLegs = legs.stream()
                .sorted(Comparator.comparing(l -> l.accountId().value()))
                .toList();

        // 4. Process legs in sorted order (debit checks, credit upserts)
        Set<AccountId> touchedAccounts = new HashSet<>();
        for (Leg leg : sortedLegs) {
            AccountId accountId = leg.accountId();
            if (leg.isDebit()) {
                if (accountId.type().requiresNonNegativeBalance()) {
                    var result = balanceRepository.upsertBalanceNonNegative(
                            accountId, leg.amount().nanoTon());
                    if (result.isEmpty()) {
                        throw new DomainException(
                                ErrorCodes.INSUFFICIENT_BALANCE,
                                "Insufficient balance on account: "
                                        + accountId,
                                Map.of("accountId", accountId.value(),
                                        "required", leg.amount().nanoTon()));
                    }
                } else {
                    balanceRepository.upsertBalanceUnchecked(
                            accountId, -leg.amount().nanoTon());
                }
            } else {
                balanceRepository.upsertBalanceUnchecked(
                        accountId, leg.amount().nanoTon());
            }
            touchedAccounts.add(accountId);
        }

        // 6. Generate txRef and insert entries
        UUID txRef = UUID.randomUUID();
        ledgerRepository.insertEntries(
                txRef, idempotencyKey, request.dealId(),
                request.description(), legs);

        // 7. Post-commit: update Redis cache for touched accounts
        registerPostCommitCacheEviction(touchedAccounts);

        // 8. Metrics
        metricsFacade.incrementCounter(MetricNames.LEDGER_ENTRY_CREATED);

        return txRef;
    }

    @Override
    @Transactional(readOnly = true)
    public long getBalance(@NonNull AccountId accountId) {
        var cached = balanceCache.get(accountId);
        if (cached.isPresent()) {
            return cached.getAsLong();
        }
        long balance = balanceRepository.getBalance(accountId);
        balanceCache.put(accountId, balance);
        return balance;
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<LedgerEntry> getEntriesByDeal(@NonNull DealId dealId) {
        return ledgerRepository.findByDealId(dealId);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull CursorPage<LedgerEntry> getEntriesByAccount(
            @NonNull AccountId accountId,
            @Nullable String cursor,
            int limit) {
        if (cursor != null) {
            try {
                Long.parseLong(cursor);
            } catch (NumberFormatException ex) {
                throw new DomainException(ErrorCodes.INVALID_CURSOR,
                        "Invalid cursor format: " + cursor);
            }
        }
        int effectiveLimit = limit > 0
                ? Math.min(limit, MAX_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;
        return ledgerRepository.findByAccountId(
                accountId, cursor, effectiveLimit);
    }

    private void validateBalance(List<Leg> legs) {
        long totalDebit = 0;
        long totalCredit = 0;
        for (Leg leg : legs) {
            if (leg.isDebit()) {
                totalDebit = Math.addExact(totalDebit,
                        leg.amount().nanoTon());
            } else {
                totalCredit = Math.addExact(totalCredit,
                        leg.amount().nanoTon());
            }
        }
        if (totalDebit != totalCredit) {
            throw new DomainException(
                    ErrorCodes.LEDGER_INCONSISTENCY,
                    "Transfer is unbalanced: debit="
                            + totalDebit + " credit=" + totalCredit);
        }
    }

    private void registerPostCommitCacheEviction(Set<AccountId> accounts) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (AccountId accountId : accounts) {
                            balanceCache.evict(accountId);
                        }
                    }
                });
    }
}
