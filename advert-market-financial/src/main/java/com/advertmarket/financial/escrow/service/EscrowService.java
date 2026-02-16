package com.advertmarket.financial.escrow.service;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import com.advertmarket.financial.api.model.DepositAddressInfo;
import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.EscrowPort;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.shared.financial.CommissionCalculator;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.util.IdempotencyKey;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Manages escrow lifecycle: deposit address generation, ledger entries
 * for funding, release (with commission), and refund.
 *
 * <p>NOT {@code @Component} — wired via
 * {@link com.advertmarket.financial.escrow.config.EscrowConfig}.
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"fenum:argument", "fenum:assignment"})
public class EscrowService implements EscrowPort {

    private final TonWalletPort tonWalletPort;
    private final LedgerPort ledgerPort;
    private final JooqTonTransactionRepository txRepository;
    private final MetricsFacade metrics;

    @Override
    public @NonNull DepositAddressInfo generateDepositAddress(
            @NonNull DealId dealId, long amountNano) {
        var addressInfo = tonWalletPort.generateDepositAddress(dealId);

        var record = new TonTransactionsRecord();
        record.setDealId(dealId.value());
        record.setDirection("IN");
        record.setAmountNano(amountNano);
        record.setToAddress(addressInfo.depositAddress());
        record.setSubwalletId((int) addressInfo.subwalletId());
        record.setStatus("PENDING");
        record.setConfirmations(0);
        record.setVersion(0);
        txRepository.save(record);

        log.info("Generated deposit address for deal={}, address={}",
                dealId, addressInfo.depositAddress());
        return addressInfo;
    }

    @Override
    public void confirmDeposit(@NonNull DealId dealId,
                               @NonNull String txHash,
                               long amountNano,
                               int confirmations,
                               @NonNull String fromAddress) {
        var amount = Money.ofNano(amountNano);
        var transfer = TransferRequest.balanced(
                dealId,
                IdempotencyKey.deposit(txHash),
                List.of(
                        new Leg(AccountId.externalTon(),
                                EntryType.ESCROW_DEPOSIT, amount,
                                Leg.Side.DEBIT),
                        new Leg(AccountId.escrow(dealId),
                                EntryType.ESCROW_DEPOSIT, amount,
                                Leg.Side.CREDIT)),
                "Deposit confirmed: " + txHash);

        ledgerPort.transfer(transfer);
        metrics.incrementCounter(MetricNames.DEPOSIT_RECEIVED);

        log.info("Escrow funded: deal={}, txHash={}, amount={}",
                dealId, txHash, amount);
    }

    @Override
    public void releaseEscrow(@NonNull DealId dealId,
                              @NonNull UserId ownerId,
                              long dealAmountNano,
                              int commissionRateBp) {
        var dealAmount = Money.ofNano(dealAmountNano);
        var result = CommissionCalculator.calculate(
                dealAmount, commissionRateBp);

        var legs = new java.util.ArrayList<>(List.of(
                new Leg(AccountId.escrow(dealId),
                        EntryType.ESCROW_RELEASE, dealAmount,
                        Leg.Side.DEBIT),
                new Leg(AccountId.commission(dealId),
                        EntryType.PLATFORM_COMMISSION,
                        result.commission(), Leg.Side.CREDIT),
                new Leg(AccountId.ownerPending(ownerId),
                        EntryType.OWNER_PAYOUT,
                        result.ownerPayout(), Leg.Side.CREDIT)));

        var transfer = new TransferRequest(
                dealId,
                IdempotencyKey.release(dealId),
                legs,
                "Escrow released for deal " + dealId);

        ledgerPort.transfer(transfer);
        metrics.incrementCounter(MetricNames.COMMISSION_CALCULATED);

        log.info("Escrow released: deal={}, commission={}, payout={}",
                dealId, result.commission(), result.ownerPayout());
    }

    @Override
    public void refundEscrow(@NonNull DealId dealId, long amountNano) {
        var amount = Money.ofNano(amountNano);
        var transfer = TransferRequest.balanced(
                dealId,
                IdempotencyKey.refund(dealId),
                List.of(
                        new Leg(AccountId.escrow(dealId),
                                EntryType.ESCROW_REFUND, amount,
                                Leg.Side.DEBIT),
                        new Leg(AccountId.externalTon(),
                                EntryType.ESCROW_REFUND, amount,
                                Leg.Side.CREDIT)),
                "Escrow refunded for deal " + dealId);

        ledgerPort.transfer(transfer);
        metrics.incrementCounter(MetricNames.REFUND_COMPLETED);

        log.info("Escrow refunded: deal={}, amount={}", dealId, amount);
    }
}
