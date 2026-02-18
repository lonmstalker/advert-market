package com.advertmarket.financial.ton.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import com.advertmarket.financial.api.event.ExecutePayoutCommand;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PayoutExecutorWorker")
class PayoutExecutorWorkerTest {

    private TonWalletPort tonWalletPort;
    private LedgerPort ledgerPort;
    private UserRepository userRepository;
    private OutboxRepository outboxRepository;
    private DistributedLockPort lockPort;
    private JsonFacade jsonFacade;
    private MetricsFacade metrics;
    private JooqTonTransactionRepository txRepository;
    private PayoutExecutorWorker worker;

    @BeforeEach
    void setUp() {
        tonWalletPort = mock(TonWalletPort.class);
        ledgerPort = mock(LedgerPort.class);
        userRepository = mock(UserRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        lockPort = mock(DistributedLockPort.class);
        jsonFacade = mock(JsonFacade.class);
        metrics = mock(MetricsFacade.class);
        txRepository = mock(JooqTonTransactionRepository.class);
        worker = new PayoutExecutorWorker(
                tonWalletPort,
                ledgerPort,
                userRepository,
                outboxRepository,
                lockPort,
                jsonFacade,
                metrics,
                txRepository);
    }

    @Test
    @DisplayName("should resume CREATED outbound payout without tx hash")
    void shouldResumeCreatedOutboundPayoutWithoutHash() {
        var dealId = DealId.generate();
        var command = new ExecutePayoutCommand(
                42L, 1_000_000_000L, 100_000_000L, 11);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_PAYOUT, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(invocation -> invocation
                        .<java.util.function.Supplier<?>>getArgument(2)
                        .get());
        when(userRepository.findTonAddress(any()))
                .thenReturn(Optional.of("UQ-owner-address"));

        var existing = new TonTransactionsRecord();
        existing.setId(555L);
        existing.setStatus("CREATED");
        existing.setTxHash(null);
        existing.setVersion(0);
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "PAYOUT"))
                .thenReturn(Optional.of(existing));
        when(tonWalletPort.submitTransaction(11, "UQ-owner-address", 1_000_000_000L))
                .thenReturn("txhash-resumed");
        when(txRepository.markSubmitted(555L, "txhash-resumed", 0))
                .thenReturn(true);
        when(ledgerPort.transfer(any(TransferRequest.class)))
                .thenReturn(UUID.randomUUID());
        when(jsonFacade.toJson(any())).thenReturn("{}");

        worker.executePayout(envelope);

        verify(tonWalletPort).submitTransaction(11, "UQ-owner-address", 1_000_000_000L);
        verify(txRepository).markSubmitted(555L, "txhash-resumed", 0);
    }

    @Test
    @DisplayName("should submit TX, record ledger, and publish PayoutCompletedEvent on success")
    void shouldCompletePayoutSuccessfully() {
        var dealId = DealId.generate();
        var command = new ExecutePayoutCommand(
                42L, 1_000_000_000L, 100_000_000L, 11);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_PAYOUT, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(userRepository.findTonAddress(new UserId(42L)))
                .thenReturn(Optional.of("UQ-owner-address"));
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "PAYOUT"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "PAYOUT", 1_000_000_000L, "UQ-owner-address", 11))
                .thenReturn(100L);
        when(txRepository.markSubmitted(100L, "txhash123", 0))
                .thenReturn(true);
        when(tonWalletPort.submitTransaction(11, "UQ-owner-address", 1_000_000_000L))
                .thenReturn("txhash123");
        when(ledgerPort.transfer(any(TransferRequest.class)))
                .thenReturn(UUID.randomUUID());
        when(jsonFacade.toJson(any())).thenReturn("{}");

        worker.executePayout(envelope);

        verify(tonWalletPort).submitTransaction(11, "UQ-owner-address", 1_000_000_000L);
        verify(ledgerPort).transfer(any(TransferRequest.class));
        verify(outboxRepository).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should publish PayoutDeferredEvent when owner has no TON address")
    void shouldDeferWhenNoTonAddress() {
        var dealId = DealId.generate();
        var command = new ExecutePayoutCommand(
                99L, 1_000_000_000L, 100_000_000L, 11);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_PAYOUT, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(userRepository.findTonAddress(new UserId(99L)))
                .thenReturn(Optional.empty());
        when(jsonFacade.toJson(any())).thenReturn("{}");

        worker.executePayout(envelope);

        verify(tonWalletPort, never()).submitTransaction(anyInt(), anyString(), anyLong());
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should propagate TON_TX_FAILED as retryable without abandoning outbound payout")
    void shouldPropagateExceptionOnSubmitFailure() {
        var dealId = DealId.generate();
        var command = new ExecutePayoutCommand(
                42L, 1_000_000_000L, 100_000_000L, 11);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_PAYOUT, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(userRepository.findTonAddress(new UserId(42L)))
                .thenReturn(Optional.of("UQ-owner-address"));
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "PAYOUT"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "PAYOUT", 1_000_000_000L, "UQ-owner-address", 11))
                .thenReturn(100L);
        when(tonWalletPort.submitTransaction(11, "UQ-owner-address", 1_000_000_000L))
                .thenThrow(new DomainException(ErrorCodes.TON_TX_FAILED, "Send failed"));

        assertThatThrownBy(() -> worker.executePayout(envelope))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCodes.TON_TX_FAILED);

        verify(txRepository).incrementRetryCount(100L);
        verify(txRepository, never()).updateStatus(100L, "ABANDONED", 0, 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository, never()).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should defer payout when TON wallet is not initialized (getSeqno -13)")
    void shouldDeferWhenTonWalletNotInitialized() {
        var dealId = DealId.generate();
        var command = new ExecutePayoutCommand(
                42L, 1_000_000_000L, 100_000_000L, 11);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_PAYOUT, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(userRepository.findTonAddress(new UserId(42L)))
                .thenReturn(Optional.of("UQ-owner-address"));
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "PAYOUT"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "PAYOUT", 1_000_000_000L, "UQ-owner-address", 11))
                .thenReturn(100L);
        when(tonWalletPort.submitTransaction(11, "UQ-owner-address", 1_000_000_000L))
                .thenThrow(new DomainException(
                        ErrorCodes.TON_API_ERROR,
                        "TON Center API call failed: method=getSeqno, reason=getSeqno failed, exitCode: -13"));
        when(jsonFacade.toJson(any())).thenReturn("{}");

        assertThatCode(() -> worker.executePayout(envelope))
                .doesNotThrowAnyException();

        verify(txRepository).incrementRetryCount(100L);
        verify(txRepository, never()).updateStatus(100L, "ABANDONED", 0, 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should propagate retryable TON API error without abandoning outbound payout")
    void shouldPropagateRetryableTonApiErrorWithoutAbandoning() {
        var dealId = DealId.generate();
        var command = new ExecutePayoutCommand(
                42L, 1_000_000_000L, 100_000_000L, 11);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_PAYOUT, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(userRepository.findTonAddress(new UserId(42L)))
                .thenReturn(Optional.of("UQ-owner-address"));
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "PAYOUT"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "PAYOUT", 1_000_000_000L, "UQ-owner-address", 11))
                .thenReturn(100L);
        when(tonWalletPort.submitTransaction(11, "UQ-owner-address", 1_000_000_000L))
                .thenThrow(new DomainException(
                        ErrorCodes.TON_API_ERROR,
                        "TON Center API call failed: sendBoc"));

        assertThatThrownBy(() -> worker.executePayout(envelope))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCodes.TON_API_ERROR);

        verify(txRepository).incrementRetryCount(100L);
        verify(txRepository, never()).updateStatus(100L, "ABANDONED", 0, 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository, never()).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should unwrap wrapped retryable TON API error without abandoning outbound payout")
    void shouldUnwrapWrappedRetryableTonApiErrorWithoutAbandoning() {
        var dealId = DealId.generate();
        var command = new ExecutePayoutCommand(
                42L, 1_000_000_000L, 100_000_000L, 11);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_PAYOUT, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(userRepository.findTonAddress(new UserId(42L)))
                .thenReturn(Optional.of("UQ-owner-address"));
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "PAYOUT"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "PAYOUT", 1_000_000_000L, "UQ-owner-address", 11))
                .thenReturn(100L);
        when(tonWalletPort.submitTransaction(11, "UQ-owner-address", 1_000_000_000L))
                .thenThrow(new RuntimeException(
                        "wrapped",
                        new DomainException(
                                ErrorCodes.TON_API_ERROR,
                                "TON Center API call failed: sendBoc")));

        assertThatThrownBy(() -> worker.executePayout(envelope))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCodes.TON_API_ERROR);

        verify(txRepository).incrementRetryCount(100L);
        verify(txRepository, never()).updateStatus(100L, "ABANDONED", 0, 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository, never()).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should resume ABANDONED payout when tx hash is missing")
    void shouldResumeAbandonedWithoutHash() {
        var dealId = DealId.generate();
        var command = new ExecutePayoutCommand(
                42L, 1_000_000_000L, 100_000_000L, 11);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_PAYOUT, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(userRepository.findTonAddress(new UserId(42L)))
                .thenReturn(Optional.of("UQ-owner-address"));

        var existing = new TonTransactionsRecord();
        existing.setId(555L);
        existing.setStatus("ABANDONED");
        existing.setTxHash(null);
        existing.setVersion(1);
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "PAYOUT"))
                .thenReturn(Optional.of(existing));
        when(tonWalletPort.submitTransaction(11, "UQ-owner-address", 1_000_000_000L))
                .thenReturn("txhash-resumed-abandoned");
        when(txRepository.markSubmitted(555L, "txhash-resumed-abandoned", 1))
                .thenReturn(true);
        when(ledgerPort.transfer(any(TransferRequest.class)))
                .thenReturn(UUID.randomUUID());
        when(jsonFacade.toJson(any())).thenReturn("{}");

        worker.executePayout(envelope);

        verify(tonWalletPort).submitTransaction(11, "UQ-owner-address", 1_000_000_000L);
        verify(txRepository).markSubmitted(555L, "txhash-resumed-abandoned", 1);
        verify(ledgerPort).transfer(any(TransferRequest.class));
        verify(outboxRepository).save(any(OutboxEntry.class));
    }
}
