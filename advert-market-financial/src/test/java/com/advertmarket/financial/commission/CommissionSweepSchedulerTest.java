package com.advertmarket.financial.commission;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.config.CommissionSweepProperties;
import com.advertmarket.financial.ledger.repository.JooqAccountBalanceRepository;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommissionSweepScheduler — commission sweep operations")
class CommissionSweepSchedulerTest {

    private JooqAccountBalanceRepository balanceRepository;
    private LedgerPort ledgerPort;
    private DistributedLockPort lockPort;
    private MetricsFacade metrics;
    private CommissionSweepScheduler scheduler;

    @BeforeEach
    void setUp() {
        balanceRepository = mock(JooqAccountBalanceRepository.class);
        ledgerPort = mock(LedgerPort.class);
        lockPort = mock(DistributedLockPort.class);
        metrics = mock(MetricsFacade.class);

        var props = new CommissionSweepProperties(
                "0 0 2 * * *", 1000L, 100, Duration.ofMinutes(5));

        scheduler = new CommissionSweepScheduler(
                balanceRepository, ledgerPort, lockPort, metrics, props);
    }

    @Test
    @DisplayName("Should throw ArithmeticException when totalSwept overflows Long.MAX_VALUE")
    void shouldThrowOnOverflow() {
        var token = UUID.randomUUID().toString();
        when(lockPort.tryLock(anyString(), any(Duration.class)))
                .thenReturn(Optional.of(token));

        var account1 = AccountId.commission(new DealId(UUID.randomUUID()));
        var account2 = AccountId.commission(new DealId(UUID.randomUUID()));
        when(balanceRepository.findCommissionAccountsAboveThreshold(
                anyLong(), anyInt()))
                .thenReturn(List.of(account1, account2));

        when(ledgerPort.getBalance(account1)).thenReturn(Long.MAX_VALUE);
        when(ledgerPort.getBalance(account2)).thenReturn(1001L);
        when(ledgerPort.transfer(any())).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> scheduler.sweep())
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("Should skip sweep when lock is not acquired")
    void shouldSkipWhenLockNotAcquired() {
        when(lockPort.tryLock(anyString(), any(Duration.class)))
                .thenReturn(Optional.empty());

        scheduler.sweep();

        verify(balanceRepository, never())
                .findCommissionAccountsAboveThreshold(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Should skip sweep when no accounts above threshold")
    void shouldSkipWhenNoAccounts() {
        var token = UUID.randomUUID().toString();
        when(lockPort.tryLock(anyString(), any(Duration.class)))
                .thenReturn(Optional.of(token));
        when(balanceRepository.findCommissionAccountsAboveThreshold(
                anyLong(), anyInt()))
                .thenReturn(List.of());

        scheduler.sweep();

        verify(ledgerPort, never()).transfer(any());
    }

    @Test
    @DisplayName("Should sweep all accounts above threshold and record metric")
    void shouldSweepAllAccountsAboveThreshold() {
        var token = UUID.randomUUID().toString();
        when(lockPort.tryLock(anyString(), any(Duration.class)))
                .thenReturn(Optional.of(token));

        var account1 = AccountId.commission(new DealId(UUID.randomUUID()));
        var account2 = AccountId.commission(new DealId(UUID.randomUUID()));
        when(balanceRepository.findCommissionAccountsAboveThreshold(
                anyLong(), anyInt()))
                .thenReturn(List.of(account1, account2));

        when(ledgerPort.getBalance(account1)).thenReturn(5_000L);
        when(ledgerPort.getBalance(account2)).thenReturn(3_000L);
        when(ledgerPort.transfer(any(TransferRequest.class)))
                .thenReturn(UUID.randomUUID());

        scheduler.sweep();

        verify(ledgerPort, times(2)).transfer(any(TransferRequest.class));
        verify(metrics).incrementCounter(
                eq(MetricNames.COMMISSION_SWEEP_COUNT),
                eq("count"), eq("2"));
        verify(metrics).incrementCounter(
                eq(MetricNames.COMMISSION_SWEEP_TOTAL_NANO),
                eq(8_000.0));
    }

    @Test
    @DisplayName("Should continue sweeping remaining accounts when one fails")
    void shouldContinueSweepingWhenOneAccountFails() {
        var token = UUID.randomUUID().toString();
        when(lockPort.tryLock(anyString(), any(Duration.class)))
                .thenReturn(Optional.of(token));

        var account1 = AccountId.commission(new DealId(UUID.randomUUID()));
        var account2 = AccountId.commission(new DealId(UUID.randomUUID()));
        when(balanceRepository.findCommissionAccountsAboveThreshold(
                anyLong(), anyInt()))
                .thenReturn(List.of(account1, account2));

        when(ledgerPort.getBalance(account1))
                .thenThrow(new RuntimeException("DB timeout"));
        when(ledgerPort.getBalance(account2)).thenReturn(3_000L);
        when(ledgerPort.transfer(any(TransferRequest.class)))
                .thenReturn(UUID.randomUUID());

        scheduler.sweep();

        verify(ledgerPort, times(1)).transfer(any(TransferRequest.class));
        verify(metrics).incrementCounter(MetricNames.COMMISSION_SWEEP_FAILED);
        verify(metrics).incrementCounter(
                eq(MetricNames.COMMISSION_SWEEP_COUNT),
                eq("count"), eq("1"));
    }

    @Test
    @DisplayName("Should release lock after sweep completes")
    void shouldReleaseLockAfterSweep() {
        var token = UUID.randomUUID().toString();
        when(lockPort.tryLock(anyString(), any(Duration.class)))
                .thenReturn(Optional.of(token));
        when(balanceRepository.findCommissionAccountsAboveThreshold(
                anyLong(), anyInt()))
                .thenReturn(List.of());

        scheduler.sweep();

        verify(lockPort).unlock(anyString(), eq(token));
    }
}
