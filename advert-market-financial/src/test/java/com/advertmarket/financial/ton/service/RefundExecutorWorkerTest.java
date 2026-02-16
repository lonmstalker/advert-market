package com.advertmarket.financial.ton.service;

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
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.shared.event.EventEnvelope;
import com.advertmarket.shared.event.EventTypes;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RefundExecutorWorker")
class RefundExecutorWorkerTest {

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
                txRepository);
    }

    @Test
    @DisplayName("should fail-closed when outbound refund exists in CREATED without tx hash")
    void shouldFailClosedOnCreatedOutboundRefundWithoutHash() {
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

        assertThatThrownBy(() -> worker.executeRefund(envelope))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCodes.TON_TX_FAILED);

        verify(tonWalletPort, never())
                .submitTransaction(anyInt(), anyString(), anyLong());
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
        verify(ledgerPort).transfer(any(TransferRequest.class));
        verify(outboxRepository).save(any(OutboxEntry.class));
    }

    @Test
    @DisplayName("should propagate exception when submitTransaction fails")
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

        verify(txRepository).updateStatus(200L, "ABANDONED", 0, 0);
        verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        verify(outboxRepository, never()).save(any(OutboxEntry.class));
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
