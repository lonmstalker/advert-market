package com.advertmarket.financial.commission;

import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.config.CommissionSweepProperties;
import com.advertmarket.financial.ledger.repository.JooqAccountBalanceRepository;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.lock.LockKeys;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.util.IdempotencyKey;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily sweep of accumulated commission balances to platform treasury.
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(CommissionSweepProperties.class)
@Slf4j
@SuppressWarnings({"fenum:argument", "fenum:assignment"})
public class CommissionSweepScheduler {

    private static final String LOCK_KEY = LockKeys.COMMISSION_SWEEP;

    private final JooqAccountBalanceRepository balanceRepository;
    private final LedgerPort ledgerPort;
    private final DistributedLockPort lockPort;
    private final MetricsFacade metrics;
    private final CommissionSweepProperties props;

    @Scheduled(cron = "${app.financial.commission-sweep.cron:0 0 2 * * *}")
    public void sweep() {
        var token = lockPort.tryLock(LOCK_KEY, props.lockTtl());
        if (token.isEmpty()) {
            log.debug("Could not acquire commission sweep lock, skipping");
            return;
        }

        try {
            doSweep();
        } finally {
            lockPort.unlock(LOCK_KEY, token.get());
        }
    }

    private void doSweep() {
        var accounts = balanceRepository
                .findCommissionAccountsAboveThreshold(
                        props.dustThresholdNano(), props.batchSize());

        if (accounts.isEmpty()) {
            log.debug("No commission accounts above dust threshold");
            return;
        }

        log.info("Commission sweep starting: {} accounts found",
                accounts.size());

        String today = LocalDate.now(ZoneOffset.UTC).toString();
        long totalSwept = 0;
        int count = 0;

        for (var accountId : accounts) {
            long swept = sweepAccount(accountId, today);
            if (swept > 0) {
                totalSwept = Math.addExact(totalSwept, swept);
                count++;
            }
        }

        metrics.incrementCounter(MetricNames.COMMISSION_SWEEP_COUNT,
                "count", String.valueOf(count));
        if (totalSwept > 0) {
            metrics.incrementCounter(
                    MetricNames.COMMISSION_SWEEP_TOTAL_NANO,
                    totalSwept);
        }

        log.info("Commission sweep completed: swept {} nanoTON "
                        + "from {} accounts",
                totalSwept, count);
    }

    private long sweepAccount(AccountId accountId, String date) {
        try {
            long balance = ledgerPort.getBalance(accountId);
            if (balance <= props.dustThresholdNano()) {
                return 0;
            }

            var amount = Money.ofNano(balance);
            var transfer = new TransferRequest(
                    null,
                    IdempotencyKey.sweep(date, accountId),
                    List.of(
                            new Leg(accountId,
                                    EntryType.COMMISSION_SWEEP, amount,
                                    Leg.Side.DEBIT),
                            new Leg(AccountId.platformTreasury(),
                                    EntryType.COMMISSION_SWEEP, amount,
                                    Leg.Side.CREDIT)),
                    "Commission sweep " + date + " from " + accountId);
            ledgerPort.transfer(transfer);
            return balance;
        } catch (Exception ex) {
            log.warn("Failed to sweep commission account {}: {}",
                    accountId, ex.getMessage());
            metrics.incrementCounter(MetricNames.COMMISSION_SWEEP_FAILED);
            return 0;
        }
    }
}
