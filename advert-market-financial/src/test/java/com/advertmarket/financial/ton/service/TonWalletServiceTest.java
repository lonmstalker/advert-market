package com.advertmarket.financial.ton.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.financial.api.model.DepositAddressInfo;
import com.advertmarket.financial.api.model.TonTransactionInfo;
import com.advertmarket.financial.api.port.TonBlockchainPort;
import com.advertmarket.financial.config.TonProperties;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.sequence.SequenceAllocator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ton.ton4j.mnemonic.Mnemonic;

@DisplayName("TonWalletService — wallet operations")
class TonWalletServiceTest {

    private static final String TEST_MNEMONIC =
            "abandon abandon abandon abandon abandon abandon abandon abandon "
                    + "abandon abandon abandon abandon abandon abandon abandon abandon "
                    + "abandon abandon abandon abandon abandon abandon abandon art";

    private TonBlockchainPort blockchainPort;
    private DistributedLockPort lockPort;
    private SequenceAllocator sequenceAllocator;
    private MetricsFacade metrics;
    private TonWalletService service;

    @BeforeEach
    void setUp() {
        blockchainPort = mock(TonBlockchainPort.class);
        lockPort = mock(DistributedLockPort.class);
        sequenceAllocator = mock(SequenceAllocator.class);
        metrics = mock(MetricsFacade.class);

        var api = new TonProperties.Api("test-key", true);
        var wallet = new TonProperties.Wallet(TEST_MNEMONIC, 50);
        var deposit = new TonProperties.Deposit(
                Duration.ofSeconds(10), Duration.ofMinutes(30), 100, 5);
        var props = new TonProperties(api, wallet, deposit, "testnet",
                new TonProperties.Confirmation());

        service = new TonWalletService(blockchainPort, lockPort,
                sequenceAllocator, metrics, props);
    }

    @Nested
    @DisplayName("security")
    class Security {

        @Test
        @DisplayName("Should not expose mnemonic in exception when key derivation fails")
        void shouldNotExposeMnemonicInException() {
            String toxicMnemonic = "supersecret_word1 supersecret_word2 supersecret_word3";

            var api = new TonProperties.Api("key", true);
            var wallet = new TonProperties.Wallet(toxicMnemonic, 50);
            var deposit = new TonProperties.Deposit(
                    Duration.ofSeconds(10), Duration.ofMinutes(30), 100, 5);
            var props = new TonProperties(api, wallet, deposit, "testnet",
                    new TonProperties.Confirmation());

            try (var mnemonicMock = mockStatic(Mnemonic.class)) {
                mnemonicMock.when(() ->
                                Mnemonic.toKeyPair(any(List.class)))
                        .thenThrow(new NoSuchAlgorithmException(
                                "simulated: mnemonic=" + toxicMnemonic));

                assertThatThrownBy(() -> new TonWalletService(
                        blockchainPort, lockPort, sequenceAllocator, metrics, props))
                        .isInstanceOf(IllegalStateException.class)
                        .satisfies(ex -> {
                            String fullTrace = getFullExceptionChain(ex);
                            assertThat(fullTrace)
                                    .doesNotContain("supersecret_word1")
                                    .doesNotContain("supersecret_word2")
                                    .doesNotContain("supersecret_word3");
                        });
            }
        }

        private String getFullExceptionChain(Throwable ex) {
            var sb = new StringBuilder();
            Throwable current = ex;
            while (current != null) {
                sb.append(current.getClass().getName())
                        .append(": ")
                        .append(current.getMessage())
                        .append("\n");
                current = current.getCause();
            }
            return sb.toString();
        }
    }

    @Nested
    @DisplayName("generateDepositAddress")
    class GenerateDepositAddress {

        @Test
        @DisplayName("Should generate unique address for each subwallet ID")
        void generatesUniqueAddress() {
            when(sequenceAllocator.next()).thenReturn(100L, 101L);

            DealId deal1 = new DealId(UUID.randomUUID());
            DealId deal2 = new DealId(UUID.randomUUID());

            DepositAddressInfo info1 = service.generateDepositAddress(deal1);
            DepositAddressInfo info2 = service.generateDepositAddress(deal2);

            assertThat(info1.subwalletId()).isEqualTo(100L);
            assertThat(info2.subwalletId()).isEqualTo(101L);
            assertThat(info1.depositAddress()).isNotEmpty();
            assertThat(info2.depositAddress()).isNotEmpty();
            assertThat(info1.depositAddress()).isNotEqualTo(info2.depositAddress());
        }

        @Test
        @DisplayName("Should return non-bounceable address format")
        void returnsNonBounceableAddress() {
            when(sequenceAllocator.next()).thenReturn(42L);

            DepositAddressInfo info = service.generateDepositAddress(
                    new DealId(UUID.randomUUID()));

            assertThat(info.depositAddress()).startsWith("0Q");
        }
    }

    @Nested
    @DisplayName("submitTransaction")
    class SubmitTransaction {

        /**
         * Generates a valid TON testnet address from the same mnemonic
         * used in service setup, but with a different subwallet.
         */
        private String generateValidAddress() {
            when(sequenceAllocator.next()).thenReturn(999L);
            return service.generateDepositAddress(
                    new DealId(UUID.randomUUID())).depositAddress();
        }

        @Test
        @DisplayName("Should acquire lock, get seqno, send BoC, and save TX")
        void submitsTxWithLock() {
            String destAddress = generateValidAddress();
            int subwalletId = 42;
            when(lockPort.withLock(anyString(), any(Duration.class), any()))
                    .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
            when(blockchainPort.getSeqno(anyString())).thenReturn(5L);
            when(blockchainPort.sendBoc(anyString())).thenReturn("txhash_abc");

            String txHash = service.submitTransaction(subwalletId,
                    destAddress, 1_000_000_000L);

            assertThat(txHash).isEqualTo("txhash_abc");
            verify(lockPort).withLock(eq("ton:subwallet-tx:42"), any(), any());
        }

        @Test
        @DisplayName("Should throw when lock cannot be acquired")
        void throwsWhenLockFails() {
            String destAddress = generateValidAddress();
            when(lockPort.withLock(anyString(), any(Duration.class), any()))
                    .thenThrow(new DomainException("LOCK_ACQUISITION_FAILED",
                            "Cannot acquire lock"));

            assertThatThrownBy(() -> service.submitTransaction(42,
                    destAddress, 1_000_000_000L))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("Should retry sendBoc when it fails and seqno unchanged")
        void retries_onSendBocFailure_seqnoUnchanged() {
            String destAddress = generateValidAddress();
            when(lockPort.withLock(anyString(), any(Duration.class), any()))
                    .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
            when(blockchainPort.getSeqno(anyString())).thenReturn(5L);
            when(blockchainPort.sendBoc(anyString()))
                    .thenThrow(new DomainException("TON_API_ERROR", "timeout"))
                    .thenReturn("txhash_retry");

            String txHash = service.submitTransaction(42,
                    destAddress, 1_000_000_000L);

            assertThat(txHash).isEqualTo("txhash_retry");
            verify(blockchainPort, org.mockito.Mockito.times(2))
                    .sendBoc(anyString());
        }

        @Test
        @DisplayName("Should detect seqno advance after sendBoc failure and recover TX hash")
        void detects_seqnoAdvance_recoversTxHash() {
            String destAddress = generateValidAddress();
            when(lockPort.withLock(anyString(), any(Duration.class), any()))
                    .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
            when(blockchainPort.getSeqno(anyString()))
                    .thenReturn(5L)   // initial fetch
                    .thenReturn(6L);  // post-failure check: seqno advanced
            when(blockchainPort.sendBoc(anyString()))
                    .thenThrow(new DomainException("TON_API_ERROR", "network error"));
            when(blockchainPort.getTransactions(anyString(), eq(1)))
                    .thenReturn(List.of(new TonTransactionInfo(
                            "recovered_hash", 100L, null, destAddress,
                            1_000_000_000L, 5_000L, 1700000000L)));

            String txHash = service.submitTransaction(42,
                    destAddress, 1_000_000_000L);

            assertThat(txHash).isEqualTo("recovered_hash");
            verify(blockchainPort).getTransactions(anyString(), eq(1));
        }

        @Test
        @DisplayName("Should throw after max retries when seqno remains unchanged")
        void failsAfterMaxRetries_seqnoUnchanged() {
            String destAddress = generateValidAddress();
            when(lockPort.withLock(anyString(), any(Duration.class), any()))
                    .thenAnswer(inv -> inv.<java.util.function.Supplier<?>>getArgument(2).get());
            when(blockchainPort.getSeqno(anyString())).thenReturn(5L);
            when(blockchainPort.sendBoc(anyString()))
                    .thenThrow(new DomainException("TON_API_ERROR", "persistent failure"));

            assertThatThrownBy(() -> service.submitTransaction(42,
                    destAddress, 1_000_000_000L))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("retries");
        }
    }
}