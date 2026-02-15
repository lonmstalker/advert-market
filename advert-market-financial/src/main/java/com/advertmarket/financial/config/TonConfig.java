package com.advertmarket.financial.config;

import static com.advertmarket.db.generated.Sequences.DEAL_SUBWALLET_SEQ;

import com.advertmarket.financial.api.port.TonBlockchainPort;
import com.advertmarket.financial.ton.client.TonCenterBlockchainAdapter;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.financial.ton.service.TonWalletService;
import com.advertmarket.shared.lock.DistributedLockPort;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.pii.PiiVaultPort;
import com.advertmarket.shared.sequence.SequenceAllocator;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ton.ton4j.toncenter.TonCenter;

/**
 * Wires all TON blockchain beans.
 */
@Configuration
@EnableConfigurationProperties({TonProperties.class, TonResilienceProperties.class})
public class TonConfig {

    /** Creates the TON Center HTTP client. */
    @Bean
    public TonCenter tonCenterClient(TonProperties props) {
        var builder = TonCenter.builder()
                .apiKey(props.api().key());
        if (props.api().isTestnet()) {
            builder.testnet();
        } else {
            builder.mainnet();
        }
        return builder.build();
    }

    /** Creates the raw (undecorated) blockchain adapter. */
    @Bean
    public TonCenterBlockchainAdapter rawTonBlockchainAdapter(
            TonCenter tonCenter, MetricsFacade metrics) {
        return new TonCenterBlockchainAdapter(tonCenter, metrics);
    }

    /** Creates the resilient blockchain port with circuit breaker and bulkhead. */
    @Bean
    public TonBlockchainPort tonBlockchainPort(
            TonCenterBlockchainAdapter raw,
            CircuitBreaker tonCenterCircuitBreaker,
            Bulkhead tonCenterBulkhead) {
        return new ResilientTonBlockchainPort(raw, tonCenterCircuitBreaker, tonCenterBulkhead);
    }

    /** Creates the subwallet sequence allocator with bulk prefetch. */
    @Bean
    public SequenceAllocator subwalletSequenceAllocator(
            DSLContext dsl, TonProperties props) {
        return new SequenceAllocator(dsl, DEAL_SUBWALLET_SEQ,
                props.wallet().allocationSize());
    }

    /** Creates the wallet service for address generation and TX submission. */
    @Bean
    public TonWalletService tonWalletService(
            TonBlockchainPort tonBlockchainPort,
            JooqTonTransactionRepository txRepository,
            DistributedLockPort lockPort,
            SequenceAllocator subwalletSequenceAllocator,
            MetricsFacade metrics,
            PiiVaultPort piiVault,
            TonProperties props) {
        String decryptedMnemonic = piiVault.resolve(props.wallet().mnemonic());
        var decryptedWallet = new TonProperties.Wallet(
                decryptedMnemonic, props.wallet().allocationSize());
        var decryptedProps = new TonProperties(
                props.api(), decryptedWallet, props.deposit(), props.network());
        return new TonWalletService(tonBlockchainPort, txRepository, lockPort,
                subwalletSequenceAllocator, metrics, decryptedProps);
    }

    /**
     * Decorator that wraps {@link TonBlockchainPort} with circuit breaker and bulkhead.
     */
    private record ResilientTonBlockchainPort(
            TonCenterBlockchainAdapter delegate,
            CircuitBreaker circuitBreaker,
            Bulkhead bulkhead
    ) implements TonBlockchainPort {

        @Override
        public List<com.advertmarket.financial.api.model.TonTransactionInfo> getTransactions(
                String address, int limit) {
            return Bulkhead.decorateSupplier(bulkhead,
                    CircuitBreaker.decorateSupplier(circuitBreaker,
                            () -> delegate.getTransactions(address, limit))).get();
        }

        @Override
        public String sendBoc(String base64Boc) {
            return Bulkhead.decorateSupplier(bulkhead,
                    CircuitBreaker.decorateSupplier(circuitBreaker,
                            () -> delegate.sendBoc(base64Boc))).get();
        }

        @Override
        public long getMasterchainSeqno() {
            return Bulkhead.decorateSupplier(bulkhead,
                    CircuitBreaker.decorateSupplier(circuitBreaker,
                            delegate::getMasterchainSeqno)).get();
        }

        @Override
        public long getAddressBalance(String address) {
            return Bulkhead.decorateSupplier(bulkhead,
                    CircuitBreaker.decorateSupplier(circuitBreaker,
                            () -> delegate.getAddressBalance(address))).get();
        }

        @Override
        public int getSeqno(String address) {
            return Bulkhead.decorateSupplier(bulkhead,
                    CircuitBreaker.decorateSupplier(circuitBreaker,
                            () -> delegate.getSeqno(address))).get();
        }

        @Override
        public long estimateFee(String address, String base64Body) {
            return Bulkhead.decorateSupplier(bulkhead,
                    CircuitBreaker.decorateSupplier(circuitBreaker,
                            () -> delegate.estimateFee(address, base64Body))).get();
        }
    }
}