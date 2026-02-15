package com.advertmarket.integration.financial;

import static com.advertmarket.db.generated.tables.LedgerEntries.LEDGER_ENTRIES;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.BalanceCachePort;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.ledger.mapper.LedgerEntryMapper;
import com.advertmarket.financial.ledger.repository.JooqAccountBalanceRepository;
import com.advertmarket.financial.ledger.repository.JooqLedgerRepository;
import com.advertmarket.financial.ledger.service.LedgerService;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.SharedContainers;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.util.IdempotencyKey;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringJUnitConfig(
        classes = LedgerServiceMetricsFailureIntegrationTest.TestConfig.class)
@DisplayName("LedgerService — metrics failure integration")
class LedgerServiceMetricsFailureIntegrationTest {

    private static final long ONE_TON = 1_000_000_000L;

    @Autowired
    private LedgerPort ledgerService;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private InMemoryBalanceCache balanceCache;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
    }

    @BeforeEach
    void cleanUp() {
        DatabaseSupport.cleanFinancialTables(DatabaseSupport.dsl());
        balanceCache.clear();
    }

    @Test
    @DisplayName("Should commit transfer and evict cache even when metrics increment throws")
    void shouldCommitEvenIfMetricsFails() {
        DealId dealId = DealId.generate();
        AccountId externalTon = AccountId.externalTon();
        AccountId escrow = AccountId.escrow(dealId);

        balanceCache.put(escrow, 0L);
        assertThat(balanceCache.get(escrow)).hasValue(0L);

        UUID txRef = ledgerService.transfer(TransferRequest.balanced(
                dealId,
                IdempotencyKey.deposit("tx-metrics-fail"),
                List.of(
                        new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                        new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                "Metrics failure should not roll back financial TX"));

        assertThat(txRef).isNotNull();
        assertThat(balanceCache.get(escrow))
                .as("Cache eviction is registered via afterCommit")
                .isEmpty();

        assertThat(dsl.fetchCount(
                LEDGER_ENTRIES, LEDGER_ENTRIES.TX_REF.eq(txRef)))
                .isEqualTo(2);
        assertThat(ledgerService.getBalance(escrow))
                .isEqualTo(ONE_TON);
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            var ds = new DriverManagerDataSource();
            ds.setUrl(SharedContainers.pgJdbcUrl());
            ds.setUsername(SharedContainers.pgUsername());
            ds.setPassword(SharedContainers.pgPassword());
            return ds;
        }

        @Bean
        PlatformTransactionManager transactionManager(
                DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        DSLContext dslContext(DataSource dataSource) {
            var txAwareDs = new TransactionAwareDataSourceProxy(
                    dataSource);
            var config = new DefaultConfiguration()
                    .set(new DataSourceConnectionProvider(txAwareDs))
                    .set(SQLDialect.POSTGRES);
            return org.jooq.impl.DSL.using(config);
        }

        @Bean
        JooqLedgerRepository ledgerRepository(DSLContext dsl) {
            return new JooqLedgerRepository(
                    dsl,
                    Mappers.getMapper(LedgerEntryMapper.class));
        }

        @Bean
        JooqAccountBalanceRepository balanceRepository(
                DSLContext dsl) {
            return new JooqAccountBalanceRepository(dsl);
        }

        @Bean
        InMemoryBalanceCache balanceCache() {
            return new InMemoryBalanceCache();
        }

        @Bean
        MetricsFacade metricsFacade() {
            return new ThrowingMetricsFacade();
        }

        @Bean
        LedgerService ledgerService(
                JooqLedgerRepository ledgerRepo,
                JooqAccountBalanceRepository balanceRepo,
                InMemoryBalanceCache cache,
                MetricsFacade metrics) {
            return new LedgerService(
                    ledgerRepo, balanceRepo, cache, metrics);
        }
    }

    static final class ThrowingMetricsFacade extends MetricsFacade {

        ThrowingMetricsFacade() {
            super(new SimpleMeterRegistry());
        }

        @Override
        public void incrementCounter(@NonNull String name, String... tags) {
            throw new RuntimeException("metrics down");
        }
    }

    static class InMemoryBalanceCache implements BalanceCachePort {

        private final Map<String, Long> store =
                new ConcurrentHashMap<>();

        @Override
        public @NonNull OptionalLong get(
                @NonNull AccountId accountId) {
            Long val = store.get(accountId.value());
            return val != null
                    ? OptionalLong.of(val)
                    : OptionalLong.empty();
        }

        @Override
        public void put(@NonNull AccountId accountId,
                        long balanceNano) {
            store.put(accountId.value(), balanceNano);
        }

        @Override
        public void evict(@NonNull AccountId accountId) {
            store.remove(accountId.value());
        }

        void clear() {
            store.clear();
        }
    }
}
