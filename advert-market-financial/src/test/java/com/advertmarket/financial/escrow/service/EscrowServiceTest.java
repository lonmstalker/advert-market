package com.advertmarket.financial.escrow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.financial.api.model.DepositAddressInfo;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.api.port.TonWalletPort;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.shared.metric.MetricsFacade;
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
    }

    @Nested
    @DisplayName("confirmDeposit")
    class ConfirmDeposit {

        @Test
        @DisplayName("Should create DEBIT EXTERNAL_TON / CREDIT ESCROW transfer")
        void createsLedgerEntries() {
            var dealId = DealId.generate();
            when(ledgerPort.transfer(any())).thenReturn(UUID.randomUUID());

            service.confirmDeposit(dealId, "txhash1",
                    10_000_000_000L, 3, "fromAddr");

            var captor = ArgumentCaptor.forClass(TransferRequest.class);
            verify(ledgerPort).transfer(captor.capture());

            var request = captor.getValue();
            assertThat(request.legs()).hasSize(2);
            assertThat(request.dealId()).isEqualTo(dealId);
            assertThat(request.idempotencyKey().value())
                    .isEqualTo("deposit:txhash1");
        }
    }

    @Nested
    @DisplayName("releaseEscrow")
    class ReleaseEscrow {

        @Test
        @DisplayName("Should create 3-leg transfer: DEBIT ESCROW, CREDIT COMMISSION, CREDIT OWNER_PENDING")
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
    }
}
