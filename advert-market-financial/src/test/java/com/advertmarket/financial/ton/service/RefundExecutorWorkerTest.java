package com.advertmarket.financial.ton.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.advertmarket.financial.api.event.ExecuteRefundCommand;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.config.NetworkFeeProperties;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("RefundExecutorWorker")
class RefundExecutorWorkerTest {

    private static final long DEFAULT_FEE_NANO = 5_000_000L;

    private TonWalletPort tonWalletPort;
    private LedgerPort ledgerPort;
    private OutboxRepository outboxRepository;
    private DistributedLockPort lockPort;
    private JsonFacade jsonFacade;
    private MetricsFacade metrics;
    private JooqTonTransactionRepository txRepository;
    private RefundExecutorWorker worker;

    @BeforeEach
    void setUp() {
        tonWalletPort = mock(TonWalletPort.class);
        ledgerPort = mock(LedgerPort.class);
        outboxRepository = mock(OutboxRepository.class);
        lockPort = mock(DistributedLockPort.class);
        jsonFacade = mock(JsonFacade.class);
        metrics = mock(MetricsFacade.class);
        txRepository = mock(JooqTonTransactionRepository.class);
        worker = new RefundExecutorWorker(
                tonWalletPort,
                ledgerPort,
                outboxRepository,
                lockPort,
                jsonFacade,
                metrics,
                txRepository,
                new NetworkFeeProperties(DEFAULT_FEE_NANO));
    }

    @Test
    @DisplayName("should resume CREATED outbound refund without tx hash")
    void shouldResumeCreatedOutboundRefundWithoutHash() {
        var dealId = DealId.generate();
        var command = new ExecuteRefundCommand(
                7L,
                1_500_000_000L,
                "UQ-advertiser-address",
                33);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(invocation -> invocation
                        .<java.util.function.Supplier<?>>getArgument(2)
                        .get());

        var existing = new TonTransactionsRecord();
        existing.setId(777L);
        existing.setStatus("CREATED");
        existing.setTxHash(null);
        existing.setVersion(0);
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "REFUND"))
                .thenReturn(Optional.of(existing));
        when(tonWalletPort.submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L))
                .thenReturn("refund-resumed");
        when(txRepository.markSubmitted(777L, "refund-resumed", 0))
                .thenReturn(true);
        when(ledgerPort.transfer(any(TransferRequest.class)))
                .thenReturn(UUID.randomUUID());
        when(jsonFacade.toJson(any())).thenReturn("{}");

        worker.executeRefund(envelope);

        verify(tonWalletPort).submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L);
        verify(txRepository).markSubmitted(777L, "refund-resumed", 0);
    }

    @Test
    @DisplayName("should submit TX, record ledger, and publish RefundCompletedEvent on success")
    void shouldCompleteRefundSuccessfully() {
        var dealId = DealId.generate();
        var command = new ExecuteRefundCommand(
                7L, 1_500_000_000L, "UQ-advertiser-address", 33);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "REFUND"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "REFUND", 1_500_000_000L, "UQ-advertiser-address", 33))
                .thenReturn(200L);
        when(txRepository.markSubmitted(200L, "refund-tx-hash", 0))
                .thenReturn(true);
        when(tonWalletPort.submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L))
                .thenReturn("refund-tx-hash");
        when(ledgerPort.transfer(any(TransferRequest.class)))
                .thenReturn(UUID.randomUUID());
        when(jsonFacade.toJson(any())).thenReturn("{}");

        worker.executeRefund(envelope);

        verify(tonWalletPort).submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L);
        var requestCaptor = ArgumentCaptor.forClass(TransferRequest.class);
        verify(ledgerPort).transfer(requestCaptor.capture());
        assertThat(requestCaptor.getValue().legs()).satisfies(legs -> {
            assertThat(legs).anySatisfy(leg -> {
                assertThat(leg.accountId())
                        .isEqualTo(AccountId.externalTon());
                assertThat(leg.entryType())
                        .isEqualTo(EntryType.ESCROW_REFUND);
                assertThat(leg.amount().nanoTon())
                        .isEqualTo(1_500_000_000L - DEFAULT_FEE_NANO);
            });
            assertThat(legs).anySatisfy(leg -> {
                assertThat(leg.accountId())
                        .isEqualTo(AccountId.networkFees());
                assertThat(leg.entryType())
                        .isEqualTo(EntryType.NETWORK_FEE_REFUND);
                assertThat(leg.amount().nanoTon())
                        .isEqualTo(DEFAULT_FEE_NANO);
            });
        });
        verify(outboxRepository).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should defer refund when wallet returns blank tx hash")
    void shouldDeferWhenWalletReturnsBlankTxHash() {
        var dealId = DealId.generate();
        var command = new ExecuteRefundCommand(
                7L, 1_500_000_000L, "UQ-advertiser-address", 33);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "REFUND"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "REFUND", 1_500_000_000L, "UQ-advertiser-address", 33))
                .thenReturn(200L);
        when(txRepository.markSubmitted(200L, "", 0))
                .thenReturn(true);
        when(tonWalletPort.submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L))
                .thenReturn("");
        when(jsonFacade.toJson(any())).thenReturn("{}");

        assertThatCode(() -> worker.executeRefund(envelope))
                .doesNotThrowAnyException();

        verify(txRepository).markSubmitted(200L, "", 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(txRepository, never()).updateStatus(200L, "CONFIRMED", 0, 1);
        verify(outboxRepository).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should propagate TON_TX_FAILED as retryable without abandoning outbound refund")
    void shouldPropagateExceptionOnSubmitFailure() {
        var dealId = DealId.generate();
        var command = new ExecuteRefundCommand(
                7L, 1_500_000_000L, "UQ-advertiser-address", 33);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "REFUND"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "REFUND", 1_500_000_000L, "UQ-advertiser-address", 33))
                .thenReturn(200L);
        when(tonWalletPort.submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L))
                .thenThrow(new DomainException(ErrorCodes.TON_TX_FAILED, "Refund send failed"));

        assertThatThrownBy(() -> worker.executeRefund(envelope))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCodes.TON_TX_FAILED);

        verify(txRepository).incrementRetryCount(200L);
        verify(txRepository, never()).updateStatus(200L, "ABANDONED", 0, 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository, never()).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should defer refund when TON wallet is not initialized (getSeqno -13)")
    void shouldDeferWhenTonWalletNotInitialized() {
        var dealId = DealId.generate();
        var command = new ExecuteRefundCommand(
                7L, 1_500_000_000L, "UQ-advertiser-address", 33);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "REFUND"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "REFUND", 1_500_000_000L, "UQ-advertiser-address", 33))
                .thenReturn(200L);
        when(tonWalletPort.submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L))
                .thenThrow(new DomainException(
                        ErrorCodes.TON_API_ERROR,
                        "TON Center API call failed: method=getSeqno, "
                                + "reason=getSeqno failed, exitCode: -13"));
        when(jsonFacade.toJson(any())).thenReturn("{}");

        assertThatCode(() -> worker.executeRefund(envelope))
                .doesNotThrowAnyException();

        verify(txRepository).incrementRetryCount(200L);
        verify(txRepository, never()).updateStatus(200L, "ABANDONED", 0, 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should defer refund when TON getSeqno -13 is present in cause chain")
    void shouldDeferWhenTonWalletNotInitializedInCauseChain() {
        var dealId = DealId.generate();
        var command = new ExecuteRefundCommand(
                7L, 1_500_000_000L, "UQ-advertiser-address", 33);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "REFUND"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "REFUND", 1_500_000_000L, "UQ-advertiser-address", 33))
                .thenReturn(200L);
        when(tonWalletPort.submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L))
                .thenThrow(new DomainException(
                        ErrorCodes.TON_API_ERROR,
                        "TON Center API call failed: getSeqno",
                        new RuntimeException("getSeqno failed, exitCode: -13")));
        when(jsonFacade.toJson(any())).thenReturn("{}");

        assertThatCode(() -> worker.executeRefund(envelope))
                .doesNotThrowAnyException();

        verify(txRepository).incrementRetryCount(200L);
        verify(txRepository, never()).updateStatus(200L, "ABANDONED", 0, 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should propagate retryable TON API error without abandoning outbound refund")
    void shouldPropagateRetryableTonApiErrorWithoutAbandoning() {
        var dealId = DealId.generate();
        var command = new ExecuteRefundCommand(
                7L, 1_500_000_000L, "UQ-advertiser-address", 33);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "REFUND"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "REFUND", 1_500_000_000L, "UQ-advertiser-address", 33))
                .thenReturn(200L);
        when(tonWalletPort.submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L))
                .thenThrow(new DomainException(
                        ErrorCodes.TON_API_ERROR,
                        "TON Center API call failed: sendBoc"));

        assertThatThrownBy(() -> worker.executeRefund(envelope))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCodes.TON_API_ERROR);

        verify(txRepository).incrementRetryCount(200L);
        verify(txRepository, never()).updateStatus(200L, "ABANDONED", 0, 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository, never()).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should unwrap wrapped retryable TON API error without abandoning outbound refund")
    void shouldUnwrapWrappedRetryableTonApiErrorWithoutAbandoning() {
        var dealId = DealId.generate();
        var command = new ExecuteRefundCommand(
                7L, 1_500_000_000L, "UQ-advertiser-address", 33);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "REFUND"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "REFUND", 1_500_000_000L, "UQ-advertiser-address", 33))
                .thenReturn(200L);
        when(tonWalletPort.submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L))
                .thenThrow(new RuntimeException(
                        "wrapped",
                        new DomainException(
                                ErrorCodes.TON_API_ERROR,
                                "TON Center API call failed: sendBoc")));

        assertThatThrownBy(() -> worker.executeRefund(envelope))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCodes.TON_API_ERROR);

        verify(txRepository).incrementRetryCount(200L);
        verify(txRepository, never()).updateStatus(200L, "ABANDONED", 0, 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository, never()).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should propagate circuit breaker open as retryable "
            + "without abandoning outbound refund")
    void shouldPropagateCallNotPermittedWithoutAbandoning() {
        var dealId = DealId.generate();
        var command = new ExecuteRefundCommand(
                7L, 1_500_000_000L, "UQ-advertiser-address", 33);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "REFUND"))
                .thenReturn(Optional.empty());
        when(txRepository.createOutbound(
                dealId.value(), "REFUND", 1_500_000_000L, "UQ-advertiser-address", 33))
                .thenReturn(200L);
        when(tonWalletPort.submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L))
                .thenThrow(mock(CallNotPermittedException.class));

        assertThatThrownBy(() -> worker.executeRefund(envelope))
                .isInstanceOf(CallNotPermittedException.class);

        verify(txRepository).incrementRetryCount(200L);
        verify(txRepository, never()).updateStatus(200L, "ABANDONED", 0, 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository, never()).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should resume ABANDONED refund when tx hash is missing")
    void shouldResumeAbandonedWithoutHash() {
        var dealId = DealId.generate();
        var command = new ExecuteRefundCommand(
                7L, 1_500_000_000L, "UQ-advertiser-address", 33);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, dealId, command);

        when(lockPort.withLock(anyString(), any(Duration.class), any()))
                .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());

        var existing = new TonTransactionsRecord();
        existing.setId(777L);
        existing.setStatus("ABANDONED");
        existing.setTxHash(null);
        existing.setVersion(1);
        when(txRepository.findLatestOutboundByDealIdAndType(dealId.value(), "REFUND"))
                .thenReturn(Optional.of(existing));
        when(tonWalletPort.submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L))
                .thenReturn("refund-resumed-abandoned");
        when(txRepository.markSubmitted(777L, "refund-resumed-abandoned", 1))
                .thenReturn(true);
        when(ledgerPort.transfer(any(TransferRequest.class)))
                .thenReturn(UUID.randomUUID());
        when(jsonFacade.toJson(any())).thenReturn("{}");

        worker.executeRefund(envelope);

        verify(tonWalletPort).submitTransaction(33, "UQ-advertiser-address", 1_500_000_000L);
        verify(txRepository).markSubmitted(777L, "refund-resumed-abandoned", 1);
        verify(ledgerPort).transfer(any(TransferRequest.class));
        verify(outboxRepository).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should skip processing when dealId is null in envelope")
    void shouldSkipWhenDealIdNull() {
        var command = new ExecuteRefundCommand(
                7L, 1_500_000_000L, "UQ-advertiser-address", 33);
        final var envelope = EventEnvelope.create(
                EventTypes.EXECUTE_REFUND, null, command);

        worker.executeRefund(envelope);

        verify(lockPort, never()).withLock(anyString(), any(Duration.class), any());
        verify(tonWalletPort, never()).submitTransaction(anyInt(), anyString(), anyLong());
    }
}
