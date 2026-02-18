package com.advertmarket.financial.ton.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import com.advertmarket.financial.api.port.TonBlockchainPort;
import com.advertmarket.financial.config.TonProperties;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.outbox.OutboxRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DepositService")
class DepositServiceTest {

    private JooqTonTransactionRepository txRepository;
    private TonBlockchainPort blockchainPort;
    private ConfirmationPolicyService confirmationPolicyService;
    private OutboxRepository outboxRepository;
    private JsonFacade jsonFacade;
    private DepositService service;

    @BeforeEach
    void setUp() {
        txRepository = mock(JooqTonTransactionRepository.class);
        blockchainPort = mock(TonBlockchainPort.class);
        confirmationPolicyService = mock(ConfirmationPolicyService.class);
        outboxRepository = mock(OutboxRepository.class);
        jsonFacade = mock(JsonFacade.class);

        var depositProps = new TonProperties.Deposit(
                Duration.ofSeconds(10),
                Duration.ofMinutes(30),
                100,
                5);

        service = new DepositService(
                txRepository,
                blockchainPort,
                confirmationPolicyService,
                outboxRepository,
                depositProps,
                jsonFacade);
    }

    @Test
    @DisplayName("getDepositInfo returns projection when TON circuit breaker is open")
    void getDepositInfo_returnsProjection_onCircuitBreakerOpen() {
        var dealId = DealId.generate();

        var record = new TonTransactionsRecord();
        record.setDealId(dealId.value());
        record.setToAddress("UQ-test-deposit-address");
        record.setAmountNano(5_000_000_000L);
        record.setStatus("PENDING");
        record.setConfirmations(0);

        when(txRepository.findLatestInboundByDealId(dealId.value()))
                .thenReturn(Optional.of(record));
        when(blockchainPort.getTransactions("UQ-test-deposit-address", 50))
                .thenThrow(mock(CallNotPermittedException.class));
        when(confirmationPolicyService.requiredConfirmations(5_000_000_000L))
                .thenReturn(new ConfirmationRequirement(3, false));

        var infoOpt = service.getDepositInfo(dealId);

        assertThat(infoOpt).isPresent();
        var info = infoOpt.orElseThrow();
        assertThat(info.escrowAddress()).isEqualTo("UQ-test-deposit-address");
        assertThat(info.amountNano()).isEqualTo("5000000000");
        assertThat(info.requiredConfirmations()).isEqualTo(3);
        assertThat(info.receivedAmountNano()).isNull();
    }
}
