package com.advertmarket.financial.ton.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.financial.api.model.DepositAddressInfo;
import com.advertmarket.financial.api.port.TonBlockchainPort;
import com.advertmarket.financial.config.TonProperties;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.sequence.SequenceAllocator;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TonWalletService — wallet operations")
class TonWalletServiceTest {

    private static final String TEST_MNEMONIC =
            "abandon abandon abandon abandon abandon abandon abandon abandon "
                    + "abandon abandon abandon abandon abandon abandon abandon abandon "
                    + "abandon abandon abandon abandon abandon abandon abandon art";

    private TonBlockchainPort blockchainPort;
    private JooqTonTransactionRepository txRepository;
    private DistributedLockPort lockPort;
    private SequenceAllocator sequenceAllocator;
    private MetricsFacade metrics;
    private TonWalletService service;

    @BeforeEach
    void setUp() {
        blockchainPort = mock(TonBlockchainPort.class);
        txRepository = mock(JooqTonTransactionRepository.class);
        lockPort = mock(DistributedLockPort.class);
        sequenceAllocator = mock(SequenceAllocator.class);
        metrics = mock(MetricsFacade.class);

        var api = new TonProperties.Api("test-key", true);
        var wallet = new TonProperties.Wallet(TEST_MNEMONIC, 50);
        var deposit = new TonProperties.Deposit(
                Duration.ofSeconds(10), Duration.ofMinutes(30), 100);
        var props = new TonProperties(api, wallet, deposit, "testnet");

        service = new TonWalletService(blockchainPort, txRepository, lockPort,
                sequenceAllocator, metrics, props);
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
            when(blockchainPort.getSeqno(anyString())).thenReturn(5);
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
    }
}