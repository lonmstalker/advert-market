package com.advertmarket.financial.wallet.service;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.model.WalletSummary;
import com.advertmarket.financial.api.model.WithdrawalResponse;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.WalletPort;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.model.TonAddress;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.util.IdempotencyKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implements user wallet queries and withdrawal via the ledger.
 *
 * <p>NOT {@code @Component} — wired via
 * {@link com.advertmarket.financial.wallet.config.WalletConfig}.
 */
@Slf4j
@RequiredArgsConstructor
public class WalletService implements WalletPort {

    private static final long VELOCITY_WINDOW_SECONDS = 86_400L;
    private static final String WITHDRAWAL_STATUS_PENDING = "PENDING";

    private final LedgerPort ledgerPort;
    private final UserRepository userRepository;
    private final MetricsFacade metrics;
    private final long minWithdrawalNano;
    private final long dailyVelocityLimitNano;
    private final long manualApprovalThresholdNano;

    @Override
    public @NonNull WalletSummary getSummary(@NonNull UserId userId) {
        var ownerPendingAccount = AccountId.ownerPending(userId);
        long availableBalance = ledgerPort.getBalance(ownerPendingAccount);
        return new WalletSummary(0L, availableBalance, 0L);
    }

    @Override
    public @NonNull CursorPage<LedgerEntry> getTransactions(
            @NonNull UserId userId,
            @Nullable String cursor,
            int limit) {
        var ownerPendingAccount = AccountId.ownerPending(userId);
        return ledgerPort.getEntriesByAccount(
                ownerPendingAccount, cursor, limit);
    }

    @Override
    public @NonNull WithdrawalResponse withdraw(
            @NonNull UserId userId,
            long amountNano,
            @NonNull String idempotencyKey) {
        if (amountNano < minWithdrawalNano) {
            throw new DomainException(
                    ErrorCodes.WITHDRAWAL_MIN_AMOUNT,
                    "Minimum withdrawal is " + minWithdrawalNano
                            + " nanoTON");
        }
        if (amountNano > manualApprovalThresholdNano) {
            throw new DomainException(
                    ErrorCodes.WITHDRAWAL_REQUIRES_MANUAL_REVIEW,
                    "Withdrawal requires manual review above "
                            + manualApprovalThresholdNano
                            + " nanoTON");
        }

        var tonAddressStr = userRepository.findTonAddress(userId)
                .orElseThrow(() -> new DomainException(
                        ErrorCodes.WALLET_ADDRESS_REQUIRED,
                        "TON address required for withdrawal"));
        var tonAddress = validateTonAddress(tonAddressStr);

        var withdrawalKey = new IdempotencyKey(
                "withdrawal:" + idempotencyKey);
        var existingTxRef = ledgerPort.findTxRefByIdempotencyKey(
                withdrawalKey);
        if (existingTxRef.isPresent()) {
            log.info("Duplicate withdrawal: user={}, key={}, txRef={}",
                    userId.value(), idempotencyKey,
                    existingTxRef.get());
            return new WithdrawalResponse(
                    existingTxRef.get().toString(),
                    WITHDRAWAL_STATUS_PENDING,
                    amountNano, tonAddress.value());
        }

        var ownerAccount = AccountId.ownerPending(userId);
        long available = ledgerPort.getBalance(ownerAccount);
        if (available < amountNano) {
            throw new DomainException(
                    ErrorCodes.INSUFFICIENT_BALANCE,
                    "Available: " + available
                            + ", requested: " + amountNano);
        }
        long withdrawn24h = ledgerPort.sumDebitsSince(
                ownerAccount,
                EntryType.OWNER_WITHDRAWAL,
                Instant.now().minusSeconds(VELOCITY_WINDOW_SECONDS));
        long projected = Math.addExact(withdrawn24h, amountNano);
        if (projected > dailyVelocityLimitNano) {
            throw new DomainException(
                    ErrorCodes.WITHDRAWAL_VELOCITY_LIMIT_EXCEEDED,
                    "24h withdrawal velocity exceeded: used="
                            + withdrawn24h + ", requested="
                            + amountNano + ", limit="
                            + dailyVelocityLimitNano);
        }

        var amount = Money.ofNano(amountNano);
        var transfer = new TransferRequest(
                null,
                withdrawalKey,
                List.of(
                        new Leg(ownerAccount,
                                EntryType.OWNER_WITHDRAWAL, amount,
                                Leg.Side.DEBIT),
                        new Leg(AccountId.externalTon(),
                                EntryType.OWNER_WITHDRAWAL, amount,
                                Leg.Side.CREDIT)),
                "Manual withdrawal by user " + userId.value());

        UUID txRef = ledgerPort.transfer(transfer);
        metrics.incrementCounter(MetricNames.WITHDRAWAL_REQUESTED);

        log.info("Withdrawal initiated: user={}, amount={}, "
                        + "address={}, txRef={}",
                userId.value(), amountNano, tonAddress.value(), txRef);

        return new WithdrawalResponse(
                txRef.toString(), WITHDRAWAL_STATUS_PENDING,
                amountNano, tonAddress.value());
    }

    private static TonAddress validateTonAddress(String raw) {
        try {
            return new TonAddress(raw);
        } catch (IllegalArgumentException ex) {
            throw new DomainException(
                    ErrorCodes.INVALID_TON_ADDRESS,
                    "Invalid TON address: " + ex.getMessage());
        }
    }
}
