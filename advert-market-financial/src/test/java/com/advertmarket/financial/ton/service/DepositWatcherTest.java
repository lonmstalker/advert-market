package com.advertmarket.financial.ton.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import com.advertmarket.financial.api.model.TonTransactionInfo;
import com.advertmarket.financial.api.port.TonBlockchainPort;
import com.advertmarket.financial.config.TonProperties;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("DepositWatcher — polls and confirms deposits")
class DepositWatcherTest {

    private TonBlockchainPort blockchainPort;
    private JooqTonTransactionRepository txRepository;
    private ConfirmationPolicyService confirmationPolicy;
    private OutboxRepository outboxRepository;
    private DistributedLockPort lockPort;
    private JsonFacade jsonFacade;
    private MetricsFacade metrics;
    private DepositWatcher watcher;

    @BeforeEach
    void setUp() {
        blockchainPort = mock(TonBlockchainPort.class);
        txRepository = mock(JooqTonTransactionRepository.class);
        confirmationPolicy = mock(ConfirmationPolicyService.class);
        outboxRepository = mock(OutboxRepository.class);
        lockPort = mock(DistributedLockPort.class);
        jsonFacade = mock(JsonFacade.class);
        metrics = mock(MetricsFacade.class);

        var deposit = new TonProperties.Deposit(
                Duration.ofSeconds(10), Duration.ofMinutes(30), 100, 5);

        watcher = new DepositWatcher(
                blockchainPort, txRepository, confirmationPolicy,
                outboxRepository, lockPort, jsonFacade, metrics, deposit);
    }

    @Nested
    @DisplayName("pollDeposits")
    class PollDeposits {

        @Test
        @DisplayName("Should skip when lock cannot be acquired")
        void skipsWhenLockNotAcquired() {
            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.empty());

            watcher.pollDeposits();

            verify(txRepository, never()).findPendingDeposits(anyInt());
        }

        @Test
        @DisplayName("Should do nothing when no pending deposits")
        void noPendingDeposits() {
            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of());

            watcher.pollDeposits();

            verify(outboxRepository, never()).save(any());
            verify(lockPort).unlock(anyString(), eq("token-1"));
        }

        @Test
        @DisplayName("Should confirm deposit when enough confirmations (1-conf tier)")
        void confirmsDeposit() {
            var record = createPendingRecord(
                    1L, UUID.randomUUID(), "UQaddr1", 50_000_000_000L);

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record));
            when(confirmationPolicy.requiredConfirmations(50_000_000_000L))
                    .thenReturn(new ConfirmationRequirement(1, false));
            when(blockchainPort.getTransactions("UQaddr1", 10))
                    .thenReturn(List.of(new TonTransactionInfo(
                            "txhash1", 123L, "fromAddr", "UQaddr1",
                            50_000_000_000L, 1000L, nowSecs() - 60)));
            when(blockchainPort.getMasterchainSeqno()).thenReturn(105L);
            when(txRepository.updateConfirmed(
                    eq(1L), eq("txhash1"), anyInt(),
                    eq(1000L), any(OffsetDateTime.class), eq(0)))
                    .thenReturn(true);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            watcher.pollDeposits();

            verify(txRepository).updateConfirmed(
                    eq(1L), eq("txhash1"), anyInt(),
                    eq(1000L), any(OffsetDateTime.class), eq(0));
            verify(outboxRepository).save(any(OutboxEntry.class));
            verify(lockPort).unlock(anyString(), eq("token-1"));
        }

        @Test
        @DisplayName("Should skip deposit when not enough confirmations")
        void skipsInsufficientConfirmations() {
            var record = createPendingRecord(
                    2L, UUID.randomUUID(), "UQaddr2", 500_000_000_000L);
            record.setSeqno(200L); // seqno when TX was first seen

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record));
            when(confirmationPolicy.requiredConfirmations(500_000_000_000L))
                    .thenReturn(new ConfirmationRequirement(3, false));
            when(blockchainPort.getTransactions("UQaddr2", 10))
                    .thenReturn(List.of(new TonTransactionInfo(
                            "txhash2", 200L, "fromAddr", "UQaddr2",
                            500_000_000_000L, 2000L, nowSecs() - 10)));
            // masterSeqno=201, record.seqno=200 → confirmedBlocks=1, need 3
            when(blockchainPort.getMasterchainSeqno()).thenReturn(201L);

            watcher.pollDeposits();

            verify(txRepository, never()).updateConfirmed(
                    anyLong(), anyString(), anyInt(),
                    anyLong(), any(OffsetDateTime.class), anyInt());
            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should emit timeout event when deposit exceeds max poll duration")
        void emitsTimeoutEvent() {
            // created 31 minutes ago — exceeds 30m maxPollDuration
            var record = createPendingRecord(
                    3L, UUID.randomUUID(), "UQaddr3", 100_000_000_000L);
            record.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(31));

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record));
            when(confirmationPolicy.requiredConfirmations(100_000_000_000L))
                    .thenReturn(new ConfirmationRequirement(1, false));
            when(blockchainPort.getTransactions("UQaddr3", 10))
                    .thenReturn(List.of());
            when(txRepository.updateStatus(eq(3L), eq("TIMEOUT"), eq(0), eq(0)))
                    .thenReturn(true);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            watcher.pollDeposits();

            verify(txRepository).updateStatus(3L, "TIMEOUT", 0, 0);

            var captor = ArgumentCaptor.forClass(OutboxEntry.class);
            verify(outboxRepository).save(captor.capture());
            assertThat(captor.getValue().topic())
                    .isEqualTo("financial.events");
        }

        @Test
        @DisplayName("Should not publish event when CAS version conflict in updateConfirmed")
        void shouldNotPublishEventWhenCasFails() {
            var record = createPendingRecord(
                    5L, UUID.randomUUID(), "UQaddr5", 10_000_000_000L);
            record.setVersion(3);

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record));
            when(confirmationPolicy.requiredConfirmations(10_000_000_000L))
                    .thenReturn(new ConfirmationRequirement(1, false));
            when(blockchainPort.getTransactions("UQaddr5", 10))
                    .thenReturn(List.of(new TonTransactionInfo(
                            "txhash5", 400L, "fromAddr", "UQaddr5",
                            10_000_000_000L, 500L, nowSecs() - 30)));
            when(blockchainPort.getMasterchainSeqno()).thenReturn(105L);
            // CAS fails — another instance already confirmed
            when(txRepository.updateConfirmed(
                    eq(5L), eq("txhash5"), anyInt(),
                    eq(500L), any(OffsetDateTime.class), eq(3)))
                    .thenReturn(false);

            watcher.pollDeposits();

            // No outbox event published
            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should pass record version to updateConfirmed for CAS")
        void shouldPassVersionToUpdateConfirmed() {
            var record = createPendingRecord(
                    6L, UUID.randomUUID(), "UQaddr6", 5_000_000_000L);
            record.setVersion(7);

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record));
            when(confirmationPolicy.requiredConfirmations(5_000_000_000L))
                    .thenReturn(new ConfirmationRequirement(1, false));
            when(blockchainPort.getTransactions("UQaddr6", 10))
                    .thenReturn(List.of(new TonTransactionInfo(
                            "txhash6", 500L, "fromAddr", "UQaddr6",
                            5_000_000_000L, 300L, nowSecs() - 20)));
            when(blockchainPort.getMasterchainSeqno()).thenReturn(510L);
            when(txRepository.updateConfirmed(
                    eq(6L), eq("txhash6"), anyInt(),
                    eq(300L), any(OffsetDateTime.class), eq(7)))
                    .thenReturn(true);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            watcher.pollDeposits();

            // Verify version 7 was passed to CAS
            verify(txRepository).updateConfirmed(
                    eq(6L), eq("txhash6"), anyInt(),
                    eq(300L), any(OffsetDateTime.class), eq(7));
        }

        @Test
        @DisplayName("Should save seqno on first detection and return 0 confirmations")
        void savesSeqnoOnFirstDetection() {
            var record = createPendingRecord(
                    7L, UUID.randomUUID(), "UQaddr7", 1_000_000_000L);
            record.setSeqno(null); // first detection — no seqno yet

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record));
            when(confirmationPolicy.requiredConfirmations(1_000_000_000L))
                    .thenReturn(new ConfirmationRequirement(1, false));
            when(blockchainPort.getTransactions("UQaddr7", 10))
                    .thenReturn(List.of(new TonTransactionInfo(
                            "txhash7", 600L, "fromAddr", "UQaddr7",
                            1_000_000_000L, 500L, nowSecs() - 10)));
            when(blockchainPort.getMasterchainSeqno()).thenReturn(300L);

            watcher.pollDeposits();

            // Seqno saved
            verify(txRepository).updateSeqno(7L, 300L, 0);
            // 0 confirmations < 1 required, so no confirm
            verify(txRepository, never()).updateConfirmed(
                    anyLong(), anyString(), anyInt(),
                    anyLong(), any(OffsetDateTime.class), anyInt());
        }

        @Test
        @DisplayName("Should not match transaction with zero amount")
        void shouldNotMatchZeroAmount() {
            // expectedAmount = 0 to specifically test the > 0 guard
            var record = createPendingRecord(
                    8L, UUID.randomUUID(), "UQaddr8", 0L);

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record));
            when(confirmationPolicy.requiredConfirmations(anyLong()))
                    .thenReturn(new ConfirmationRequirement(1, false));
            when(blockchainPort.getTransactions("UQaddr8", 10))
                    .thenReturn(List.of(new TonTransactionInfo(
                            "txhash8", 600L, "fromAddr", "UQaddr8",
                            0L, 500L, nowSecs() - 10)));

            watcher.pollDeposits();

            verify(txRepository, never()).updateConfirmed(
                    anyLong(), anyString(), anyInt(),
                    anyLong(), any(OffsetDateTime.class), anyInt());
        }

        @Test
        @DisplayName("Should not match transaction with negative amount")
        void shouldNotMatchNegativeAmount() {
            // expectedAmount = 0 to specifically test the > 0 guard
            var record = createPendingRecord(
                    9L, UUID.randomUUID(), "UQaddr9", 0L);

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record));
            when(confirmationPolicy.requiredConfirmations(anyLong()))
                    .thenReturn(new ConfirmationRequirement(1, false));
            when(blockchainPort.getTransactions("UQaddr9", 10))
                    .thenReturn(List.of(new TonTransactionInfo(
                            "txhash9", 700L, "fromAddr", "UQaddr9",
                            -1L, 500L, nowSecs() - 10)));

            watcher.pollDeposits();

            verify(txRepository, never()).updateConfirmed(
                    anyLong(), anyString(), anyInt(),
                    anyLong(), any(OffsetDateTime.class), anyInt());
        }

        @Test
        @DisplayName("Should select most recent transaction by lt when multiple match")
        void shouldSelectMostRecentByLt() {
            var record = createPendingRecord(
                    12L, UUID.randomUUID(), "UQaddrLt", 1_000_000_000L);

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record));
            when(confirmationPolicy.requiredConfirmations(anyLong()))
                    .thenReturn(new ConfirmationRequirement(1, false));
            when(blockchainPort.getTransactions("UQaddrLt", 10))
                    .thenReturn(List.of(
                            new TonTransactionInfo(
                                    "oldTx", 100L, "from", "UQaddrLt",
                                    1_000_000_000L, 500L, nowSecs() - 120),
                            new TonTransactionInfo(
                                    "newTx", 200L, "from", "UQaddrLt",
                                    1_000_000_000L, 500L, nowSecs() - 60)));
            when(blockchainPort.getMasterchainSeqno()).thenReturn(200L);
            when(txRepository.updateConfirmed(
                    eq(12L), eq("newTx"), anyInt(),
                    eq(500L), any(OffsetDateTime.class), eq(0)))
                    .thenReturn(true);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            watcher.pollDeposits();

            // Must select "newTx" (lt=200) over "oldTx" (lt=100)
            verify(txRepository).updateConfirmed(
                    eq(12L), eq("newTx"), anyInt(),
                    eq(500L), any(OffsetDateTime.class), eq(0));
        }

        @Test
        @DisplayName("Should continue processing other deposits when one fails")
        void continuesOnError() {
            var record1 = createPendingRecord(
                    10L, UUID.randomUUID(), "UQfail", 1_000_000_000L);
            var record2 = createPendingRecord(
                    11L, UUID.randomUUID(), "UQsuccess", 1_000_000_000L);

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record1, record2));
            when(confirmationPolicy.requiredConfirmations(anyLong()))
                    .thenReturn(new ConfirmationRequirement(1, false));
            when(blockchainPort.getTransactions("UQfail", 10))
                    .thenThrow(new RuntimeException("API error"));
            when(blockchainPort.getTransactions("UQsuccess", 10))
                    .thenReturn(List.of(new TonTransactionInfo(
                            "txOk", 300L, "from", "UQsuccess",
                            1_000_000_000L, 500L, nowSecs() - 60)));
            when(blockchainPort.getMasterchainSeqno()).thenReturn(305L);
            when(txRepository.updateConfirmed(
                    eq(11L), anyString(), anyInt(),
                    anyLong(), any(OffsetDateTime.class), eq(0)))
                    .thenReturn(true);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            watcher.pollDeposits();

            // Second deposit still processed despite first failing
            verify(txRepository).updateConfirmed(
                    eq(11L), eq("txOk"), anyInt(),
                    eq(500L), any(OffsetDateTime.class), eq(0));
        }

        @Test
        @DisplayName("Should mark deposit as FAILED after max retries exceeded")
        void shouldMarkFailedAfterMaxRetries() {
            var record = createPendingRecord(
                    13L, UUID.randomUUID(), "UQfailRetry", 1_000_000_000L);

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record));
            when(confirmationPolicy.requiredConfirmations(anyLong()))
                    .thenReturn(new ConfirmationRequirement(1, false));
            when(blockchainPort.getTransactions("UQfailRetry", 10))
                    .thenThrow(new RuntimeException("API error"));
            // retry_count is already at max (5)
            when(txRepository.incrementRetryCount(13L)).thenReturn(6);
            when(txRepository.updateStatus(eq(13L), eq("FAILED"), eq(0), eq(0)))
                    .thenReturn(true);
            when(jsonFacade.toJson(any())).thenReturn("{}");

            watcher.pollDeposits();

            verify(txRepository).incrementRetryCount(13L);
            verify(txRepository).updateStatus(13L, "FAILED", 0, 0);
        }

        @Test
        @DisplayName("Should increment retry count but not fail when under max retries")
        void shouldIncrementRetryCountUnderMax() {
            var record = createPendingRecord(
                    14L, UUID.randomUUID(), "UQretry", 1_000_000_000L);

            when(lockPort.tryLock(anyString(), any(Duration.class)))
                    .thenReturn(Optional.of("token-1"));
            when(txRepository.findPendingDeposits(anyInt()))
                    .thenReturn(List.of(record));
            when(confirmationPolicy.requiredConfirmations(anyLong()))
                    .thenReturn(new ConfirmationRequirement(1, false));
            when(blockchainPort.getTransactions("UQretry", 10))
                    .thenThrow(new RuntimeException("API error"));
            when(txRepository.incrementRetryCount(14L)).thenReturn(2);

            watcher.pollDeposits();

            verify(txRepository).incrementRetryCount(14L);
            verify(txRepository, never()).updateStatus(
                    anyLong(), eq("FAILED"), anyInt(), anyInt());
        }
    }

    private TonTransactionsRecord createPendingRecord(
            long id, UUID dealId, String toAddress, long amountNano) {
        var record = new TonTransactionsRecord();
        record.setId(id);
        record.setDealId(dealId);
        record.setToAddress(toAddress);
        record.setAmountNano(amountNano);
        record.setDirection("IN");
        record.setStatus("PENDING");
        record.setConfirmations(0);
        record.setVersion(0);
        record.setSeqno(100L);
        record.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return record;
    }

    private long nowSecs() {
        return Instant.now().getEpochSecond();
    }
}
