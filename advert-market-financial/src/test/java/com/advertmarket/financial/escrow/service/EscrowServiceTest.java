package com.advertmarket.financial.escrow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.financial.api.model.DepositAddressInfo;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.UserId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("EscrowService — escrow lifecycle operations")
class EscrowServiceTest {

    private TonWalletPort tonWalletPort;
    private LedgerPort ledgerPort;
    private JooqTonTransactionRepository txRepository;
    private MetricsFacade metrics;
    private EscrowService service;

    @BeforeEach
    void setUp() {
        tonWalletPort = mock(TonWalletPort.class);
        ledgerPort = mock(LedgerPort.class);
        txRepository = mock(JooqTonTransactionRepository.class);
        metrics = mock(MetricsFacade.class);

        service = new EscrowService(tonWalletPort, ledgerPort,
                txRepository, metrics);
    }

    @Nested
    @DisplayName("generateDepositAddress")
    class GenerateDepositAddress {

        @Test
        @DisplayName("Should delegate to TonWalletPort and save transaction record")
        void delegatesAndSaves() {
            var dealId = DealId.generate();
            when(tonWalletPort.generateDepositAddress(dealId))
                    .thenReturn(new DepositAddressInfo("UQaddr", 42L));
            when(txRepository.save(any())).thenReturn(1L);

            var result = service.generateDepositAddress(dealId, 5_000_000_000L);

            assertThat(result.depositAddress()).isEqualTo("UQaddr");
            assertThat(result.subwalletId()).isEqualTo(42L);
            verify(txRepository).save(any());
        }

        @Test
        @DisplayName("Should throw when subwalletId exceeds Integer.MAX_VALUE")
        void rejectsSubwalletIdOverflow() {
            var dealId = DealId.generate();
            long overflowId = Integer.MAX_VALUE + 1L;
            when(tonWalletPort.generateDepositAddress(dealId))
                    .thenReturn(new DepositAddressInfo("UQaddr", overflowId));

            assertThatThrownBy(() ->
                    service.generateDepositAddress(dealId, 5_000_000_000L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Subwallet ID overflow");

            verify(txRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("confirmDeposit")
    class ConfirmDeposit {

        @Test
        @DisplayName("Should create 2-leg transfer for exact match")
        void exactMatch_createsLedgerEntries() {
            var dealId = DealId.generate();
            when(ledgerPort.transfer(any())).thenReturn(UUID.randomUUID());

            service.confirmDeposit(dealId, "txhash1",
                    10_000_000_000L, 10_000_000_000L, 3, "fromAddr");

            var captor = ArgumentCaptor.forClass(TransferRequest.class);
            verify(ledgerPort).transfer(captor.capture());

            var request = captor.getValue();
            assertThat(request.legs()).hasSize(2);
            assertThat(request.dealId()).isEqualTo(dealId);
            assertThat(request.idempotencyKey().value())
                    .isEqualTo("deposit:txhash1");
        }

        @Test
        @DisplayName("Should create 3-leg transfer for overpayment: ESCROW + OVERPAYMENT")
        void overpayment_splitLegs() {
            var dealId = DealId.generate();
            when(ledgerPort.transfer(any())).thenReturn(UUID.randomUUID());
            long expected = 10_000_000_000L;
            long received = 12_000_000_000L;

            service.confirmDeposit(dealId, "txhash_over",
                    received, expected, 5, "fromAddr");

            var captor = ArgumentCaptor.forClass(TransferRequest.class);
            verify(ledgerPort).transfer(captor.capture());

            var request = captor.getValue();
            assertThat(request.legs()).hasSize(3);

            // DEBIT EXTERNAL_TON for full received amount
            assertThat(request.legs().get(0).accountId())
                    .isEqualTo(AccountId.externalTon());
            assertThat(request.legs().get(0).amount().nanoTon())
                    .isEqualTo(received);

            // CREDIT ESCROW for expected amount
            assertThat(request.legs().get(1).accountId())
                    .isEqualTo(AccountId.escrow(dealId));
            assertThat(request.legs().get(1).amount().nanoTon())
                    .isEqualTo(expected);

            // CREDIT OVERPAYMENT for excess
            assertThat(request.legs().get(2).accountId())
                    .isEqualTo(AccountId.overpayment(dealId));
            assertThat(request.legs().get(2).amount().nanoTon())
                    .isEqualTo(2_000_000_000L);
        }

        @Test
        @DisplayName("Should use deterministic idempotency key based on txHash")
        void idempotencyKeyBasedOnTxHash() {
            var dealId = DealId.generate();
            when(ledgerPort.transfer(any())).thenReturn(UUID.randomUUID());

            service.confirmDeposit(dealId, "txhash_dup",
                    10_000_000_000L, 10_000_000_000L, 3, "fromAddr");
            service.confirmDeposit(dealId, "txhash_dup",
                    10_000_000_000L, 10_000_000_000L, 3, "fromAddr");

            var captor = ArgumentCaptor.forClass(TransferRequest.class);
            verify(ledgerPort, org.mockito.Mockito.times(2))
                    .transfer(captor.capture());

            assertThat(captor.getAllValues())
                    .extracting(r -> r.idempotencyKey().value())
                    .containsOnly("deposit:txhash_dup");
        }
    }

    @Nested
    @DisplayName("releaseEscrow")
    class ReleaseEscrow {

        @Test
        @DisplayName("Should create 3-leg transfer: DEBIT ESCROW, "
                + "CREDIT COMMISSION, CREDIT OWNER_PENDING")
        void threeLegTransfer() {
            var dealId = DealId.generate();
            var ownerId = new UserId(123L);
            when(ledgerPort.transfer(any())).thenReturn(UUID.randomUUID());

            // 10 TON deal, 5% commission (500 bp)
            service.releaseEscrow(dealId, ownerId, 10_000_000_000L, 500);

            var captor = ArgumentCaptor.forClass(TransferRequest.class);
            verify(ledgerPort).transfer(captor.capture());

            var request = captor.getValue();
            assertThat(request.legs()).hasSize(3);
            assertThat(request.idempotencyKey().value())
                    .isEqualTo("release:" + dealId.value());
        }

        @Test
        @DisplayName("Should use deterministic idempotency key based on dealId")
        void idempotencyKeyBasedOnDealId() {
            var dealId = DealId.generate();
            var ownerId = new UserId(123L);
            when(ledgerPort.transfer(any())).thenReturn(UUID.randomUUID());

            service.releaseEscrow(dealId, ownerId, 10_000_000_000L, 500);
            service.releaseEscrow(dealId, ownerId, 10_000_000_000L, 500);

            var captor = ArgumentCaptor.forClass(TransferRequest.class);
            verify(ledgerPort, org.mockito.Mockito.times(2))
                    .transfer(captor.capture());

            assertThat(captor.getAllValues())
                    .extracting(r -> r.idempotencyKey().value())
                    .containsOnly("release:" + dealId.value());
        }

        @Test
        @DisplayName("Should skip commission leg when commission rate is zero")
        void zeroCommission_skipsCommissionLeg() {
            var dealId = DealId.generate();
            var ownerId = new UserId(123L);
            when(ledgerPort.transfer(any())).thenReturn(UUID.randomUUID());

            service.releaseEscrow(dealId, ownerId, 10_000_000_000L, 0);

            var captor = ArgumentCaptor.forClass(TransferRequest.class);
            verify(ledgerPort).transfer(captor.capture());

            var request = captor.getValue();
            assertThat(request.legs()).hasSize(2);
            assertThat(request.legs())
                    .noneMatch(leg ->
                            leg.accountId().equals(AccountId.commission(dealId)));
            assertThat(request.idempotencyKey().value())
                    .isEqualTo("release:" + dealId.value());
        }
    }

    @Nested
    @DisplayName("refundEscrow")
    class RefundEscrow {

        @Test
        @DisplayName("Should create DEBIT ESCROW / CREDIT EXTERNAL_TON transfer")
        void refundTransfer() {
            var dealId = DealId.generate();
            when(ledgerPort.transfer(any())).thenReturn(UUID.randomUUID());

            service.refundEscrow(dealId, 5_000_000_000L);

            var captor = ArgumentCaptor.forClass(TransferRequest.class);
            verify(ledgerPort).transfer(captor.capture());

            var request = captor.getValue();
            assertThat(request.legs()).hasSize(2);
            assertThat(request.idempotencyKey().value())
                    .isEqualTo("refund:" + dealId.value());
        }

        @Test
        @DisplayName("Should use deterministic idempotency key based on dealId")
        void idempotencyKeyBasedOnDealId() {
            var dealId = DealId.generate();
            when(ledgerPort.transfer(any())).thenReturn(UUID.randomUUID());

            service.refundEscrow(dealId, 5_000_000_000L);
            service.refundEscrow(dealId, 5_000_000_000L);

            var captor = ArgumentCaptor.forClass(TransferRequest.class);
            verify(ledgerPort, org.mockito.Mockito.times(2))
                    .transfer(captor.capture());

            assertThat(captor.getAllValues())
                    .extracting(r -> r.idempotencyKey().value())
                    .containsOnly("refund:" + dealId.value());
        }
    }
}
