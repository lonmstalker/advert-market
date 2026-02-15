package com.advertmarket.integration.financial;

import static com.advertmarket.db.generated.tables.AccountBalances.ACCOUNT_BALANCES;
import static com.advertmarket.db.generated.tables.LedgerEntries.LEDGER_ENTRIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jooq.impl.DSL.sum;

import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.BalanceCachePort;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.financial.ledger.repository.JooqAccountBalanceRepository;
import com.advertmarket.financial.ledger.repository.JooqLedgerRepository;
import com.advertmarket.financial.ledger.service.LedgerService;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.SharedContainers;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.util.IdempotencyKey;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Integration tests for the double-entry ledger service.
 *
 * <p>Uses real PostgreSQL via {@link SharedContainers} with Spring transaction management.
 */
@SpringJUnitConfig(classes = LedgerServiceIntegrationTest.TestConfig.class)
@DisplayName("LedgerService — integration")
class LedgerServiceIntegrationTest {

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

    @Nested
    @DisplayName("Transfer recording")
    class TransferRecording {

        @Test
        @DisplayName("Should record a balanced two-leg deposit transfer")
        void twoLegDeposit() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-hash-001"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    "Test deposit");

            UUID txRef = ledgerService.transfer(request);

            assertThat(txRef).isNotNull();
            assertThat(ledgerService.getBalance(externalTon))
                    .isEqualTo(-ONE_TON);
            assertThat(ledgerService.getBalance(escrow))
                    .isEqualTo(ONE_TON);

            List<LedgerEntry> entries = ledgerService.getEntriesByDeal(dealId);
            assertThat(entries).hasSize(2);
            assertThat(entries).allSatisfy(e -> {
                assertThat(e.txRef()).isEqualTo(txRef);
                assertThat(e.idempotencyKey())
                        .isEqualTo("deposit:tx-hash-001");
            });
        }

        @Test
        @DisplayName("Should record a multi-leg escrow release transfer")
        void multiLegRelease() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId ownerPending = AccountId.ownerPending(
                    new com.advertmarket.shared.model.UserId(100L));
            AccountId treasury = AccountId.platformTreasury();

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-deposit"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            long commission = 100_000_000L;
            long ownerAmount = ONE_TON - commission;

            UUID releaseTxRef = ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(ownerPending, EntryType.OWNER_PAYOUT,
                                    Money.ofNano(ownerAmount), Leg.Side.CREDIT),
                            new Leg(treasury, EntryType.PLATFORM_COMMISSION,
                                    Money.ofNano(commission), Leg.Side.CREDIT)),
                    "Release with commission"));

            assertThat(releaseTxRef).isNotNull();
            assertThat(ledgerService.getBalance(escrow)).isZero();
            assertThat(ledgerService.getBalance(ownerPending))
                    .isEqualTo(ownerAmount);
            assertThat(ledgerService.getBalance(treasury))
                    .isEqualTo(commission);

            List<LedgerEntry> releaseEntries = ledgerService
                    .getEntriesByDeal(dealId);
            assertThat(releaseEntries).hasSize(5);
        }

        @Test
        @DisplayName("Should persist entries and balances atomically")
        void atomicPersistence() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);

            UUID txRef = ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-atomic"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            Long dbBalance = dsl.select(ACCOUNT_BALANCES.BALANCE_NANO)
                    .from(ACCOUNT_BALANCES)
                    .where(ACCOUNT_BALANCES.ACCOUNT_ID.eq(escrow.value()))
                    .fetchOne(ACCOUNT_BALANCES.BALANCE_NANO);
            assertThat(dbBalance).isEqualTo(ONE_TON);

            int entryCount = dsl.selectCount()
                    .from(LEDGER_ENTRIES)
                    .where(LEDGER_ENTRIES.TX_REF.eq(txRef))
                    .fetchOne(0, int.class);
            assertThat(entryCount).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Idempotency")
    class Idempotency {

        @Test
        @DisplayName("Should return existing txRef on duplicate idempotency key")
        void duplicateIdempotencyKey() {
            DealId dealId = DealId.generate();
            IdempotencyKey key = IdempotencyKey.deposit("tx-duplicate");
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);

            TransferRequest request = TransferRequest.balanced(
                    dealId, key,
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null);

            UUID firstTxRef = ledgerService.transfer(request);
            UUID secondTxRef = ledgerService.transfer(request);

            assertThat(secondTxRef).isEqualTo(firstTxRef);
            assertThat(ledgerService.getBalance(escrow)).isEqualTo(ONE_TON);
        }

        @Test
        @DisplayName("Should reject concurrent double-spend via idempotency")
        void concurrentDoubleSpend() throws Exception {
            DealId dealId = DealId.generate();
            IdempotencyKey key = IdempotencyKey.deposit("tx-concurrent");
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);

            TransferRequest request = TransferRequest.balanced(
                    dealId, key,
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null);

            int threads = 5;
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger();
            ConcurrentHashMap<UUID, Boolean> txRefs = new ConcurrentHashMap<>();

            try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
                List<Future<?>> futures = new java.util.ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    futures.add(executor.submit(() -> {
                        try {
                            latch.await();
                            UUID ref = ledgerService.transfer(request);
                            txRefs.put(ref, true);
                            successCount.incrementAndGet();
                        } catch (Exception ignored) {
                        }
                    }));
                }
                latch.countDown();
                for (Future<?> f : futures) {
                    f.get();
                }
            }

            assertThat(txRefs).hasSize(1);
            assertThat(successCount.get()).isEqualTo(threads);
            assertThat(ledgerService.getBalance(escrow)).isEqualTo(ONE_TON);
        }

        @Test
        @DisplayName("Should not deadlock with overlapping accounts across concurrent transfers")
        void concurrentOverlappingTransfers() throws Exception {
            DealId dealIdA = DealId.generate();
            DealId dealIdB = DealId.generate();
            AccountId escrowA = AccountId.escrow(dealIdA);
            AccountId escrowB = AccountId.escrow(dealIdB);
            AccountId externalTon = AccountId.externalTon();

            ledgerService.transfer(TransferRequest.balanced(
                    dealIdA,
                    IdempotencyKey.deposit("tx-overlap-seed-a"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(10 * ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrowA, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(10 * ONE_TON), Leg.Side.CREDIT)),
                    null));
            ledgerService.transfer(TransferRequest.balanced(
                    dealIdB,
                    IdempotencyKey.deposit("tx-overlap-seed-b"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(10 * ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrowB, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(10 * ONE_TON), Leg.Side.CREDIT)),
                    null));

            int threads = 10;
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger();
            AtomicInteger failCount = new AtomicInteger();

            try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
                List<Future<?>> futures = new java.util.ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    int idx = i;
                    futures.add(executor.submit(() -> {
                        try {
                            latch.await();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        AccountId fromEscrow = idx % 2 == 0
                                ? escrowA : escrowB;
                        AccountId toEscrow = idx % 2 == 0
                                ? escrowB : escrowA;
                        DealId fromDeal = idx % 2 == 0
                                ? dealIdA : dealIdB;
                        try {
                            ledgerService.transfer(TransferRequest.balanced(
                                    fromDeal,
                                    IdempotencyKey.release(
                                            DealId.of(UUID.randomUUID())),
                                    List.of(
                                            new Leg(fromEscrow,
                                                    EntryType.ESCROW_RELEASE,
                                                    Money.ofNano(ONE_TON),
                                                    Leg.Side.DEBIT),
                                            new Leg(toEscrow,
                                                    EntryType.ESCROW_DEPOSIT,
                                                    Money.ofNano(ONE_TON),
                                                    Leg.Side.CREDIT)),
                                    null));
                            successCount.incrementAndGet();
                        } catch (DomainException ex) {
                            failCount.incrementAndGet();
                        }
                    }));
                }
                latch.countDown();
                for (Future<?> f : futures) {
                    f.get();
                }
            }

            long balanceA = ledgerService.getBalance(escrowA);
            long balanceB = ledgerService.getBalance(escrowB);
            assertThat(balanceA + balanceB)
                    .as("Total across escrows must be conserved")
                    .isEqualTo(20 * ONE_TON);
            assertThat(successCount.get() + failCount.get())
                    .isEqualTo(threads);
        }
    }

    @Nested
    @DisplayName("Balance checks")
    class BalanceChecks {

        @Test
        @DisplayName("Should throw INSUFFICIENT_BALANCE when account lacks funds")
        void insufficientBalance() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId ownerPending = AccountId.ownerPending(
                    new com.advertmarket.shared.model.UserId(200L));

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(ownerPending, EntryType.OWNER_PAYOUT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INSUFFICIENT_BALANCE"));
        }

        @Test
        @DisplayName("Should allow negative balance for EXTERNAL_TON account")
        void negativeBalanceForContraAccount() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-negative-1"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            DealId dealId2 = DealId.generate();
            ledgerService.transfer(TransferRequest.balanced(
                    dealId2,
                    IdempotencyKey.deposit("tx-negative-2"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(2 * ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.escrow(dealId2),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(2 * ONE_TON), Leg.Side.CREDIT)),
                    null));

            assertThat(ledgerService.getBalance(externalTon))
                    .isEqualTo(-3 * ONE_TON);
        }

        @Test
        @DisplayName("Should handle initial balance creation for new account")
        void initialBalanceCreation() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);

            assertThat(ledgerService.getBalance(escrow)).isZero();

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-initial"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(500_000_000L), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(500_000_000L), Leg.Side.CREDIT)),
                    null));

            assertThat(ledgerService.getBalance(escrow))
                    .isEqualTo(500_000_000L);
        }

        @Test
        @DisplayName("Should correctly debit after credit on same account")
        void creditThenDebit() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId externalTon = AccountId.externalTon();
            AccountId ownerPending = AccountId.ownerPending(
                    new com.advertmarket.shared.model.UserId(300L));

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-cd-1"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(ownerPending, EntryType.OWNER_PAYOUT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            assertThat(ledgerService.getBalance(escrow)).isZero();
            assertThat(ledgerService.getBalance(ownerPending))
                    .isEqualTo(ONE_TON);
        }
    }

    @Nested
    @DisplayName("Accounting invariants")
    class AccountingInvariants {

        @Test
        @DisplayName("Should maintain SUM(debit) == SUM(credit) across all entries")
        void globalBalanceInvariant() {
            for (int i = 0; i < 5; i++) {
                DealId dealId = DealId.generate();
                long amount = (i + 1) * 100_000_000L;
                ledgerService.transfer(TransferRequest.balanced(
                        dealId,
                        IdempotencyKey.deposit("tx-inv-" + i),
                        List.of(
                                new Leg(AccountId.externalTon(),
                                        EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(amount), Leg.Side.DEBIT),
                                new Leg(AccountId.escrow(dealId),
                                        EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(amount), Leg.Side.CREDIT)),
                        null));
            }

            var totalDebit = sum(LEDGER_ENTRIES.DEBIT_NANO);
            var totalCredit = sum(LEDGER_ENTRIES.CREDIT_NANO);
            var totals = dsl.select(totalDebit, totalCredit)
                    .from(LEDGER_ENTRIES)
                    .fetchOne();

            assertThat(totals).isNotNull();
            assertThat(totals.get(totalDebit))
                    .isEqualTo(totals.get(totalCredit));
        }

        @Test
        @DisplayName("Should maintain per-txRef SUM(debit) == SUM(credit)")
        void perTransactionInvariant() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId ownerPending = AccountId.ownerPending(
                    new com.advertmarket.shared.model.UserId(400L));
            AccountId treasury = AccountId.platformTreasury();

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-per-1"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            long commission = 100_000_000L;
            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(ownerPending, EntryType.OWNER_PAYOUT,
                                    Money.ofNano(ONE_TON - commission),
                                    Leg.Side.CREDIT),
                            new Leg(treasury, EntryType.PLATFORM_COMMISSION,
                                    Money.ofNano(commission), Leg.Side.CREDIT)),
                    null));

            var debitSum = sum(LEDGER_ENTRIES.DEBIT_NANO);
            var creditSum = sum(LEDGER_ENTRIES.CREDIT_NANO);
            var results = dsl.select(
                            LEDGER_ENTRIES.TX_REF, debitSum, creditSum)
                    .from(LEDGER_ENTRIES)
                    .groupBy(LEDGER_ENTRIES.TX_REF)
                    .fetch();

            assertThat(results).hasSize(2);
            results.forEach(r ->
                    assertThat(r.get(debitSum)).isEqualTo(r.get(creditSum)));
        }

        @Test
        @DisplayName("Should keep balance consistent with SUM(credits) - SUM(debits)")
        void balanceConsistency() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-bc-1"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            var creditAgg = sum(LEDGER_ENTRIES.CREDIT_NANO);
            var debitAgg = sum(LEDGER_ENTRIES.DEBIT_NANO);
            var accountSums = dsl.select(
                            LEDGER_ENTRIES.ACCOUNT_ID, creditAgg, debitAgg)
                    .from(LEDGER_ENTRIES)
                    .groupBy(LEDGER_ENTRIES.ACCOUNT_ID)
                    .fetch();

            for (var row : accountSums) {
                String accountId = row.get(LEDGER_ENTRIES.ACCOUNT_ID);
                long credits = row.get(creditAgg).longValue();
                long debits = row.get(debitAgg).longValue();
                long expectedBalance = credits - debits;

                long actualBalance = dsl.select(ACCOUNT_BALANCES.BALANCE_NANO)
                        .from(ACCOUNT_BALANCES)
                        .where(ACCOUNT_BALANCES.ACCOUNT_ID.eq(accountId))
                        .fetchOne(ACCOUNT_BALANCES.BALANCE_NANO);
                assertThat(actualBalance)
                        .as("Balance for account " + accountId)
                        .isEqualTo(expectedBalance);
            }
        }
    }

    @Nested
    @DisplayName("Query operations")
    class QueryOperations {

        @Test
        @DisplayName("Should return entries by deal sorted by created_at DESC")
        void entriesByDeal() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-deal-1"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            List<LedgerEntry> entries = ledgerService.getEntriesByDeal(dealId);
            assertThat(entries)
                    .hasSize(2)
                    .allSatisfy(e -> assertThat(e.dealId()).isEqualTo(dealId));

            DealId otherDeal = DealId.generate();
            assertThat(ledgerService.getEntriesByDeal(otherDeal)).isEmpty();
        }

        @Test
        @DisplayName("Should paginate entries by account with cursor")
        void cursorPagination() {
            AccountId externalTon = AccountId.externalTon();

            for (int i = 0; i < 7; i++) {
                DealId dealId = DealId.generate();
                ledgerService.transfer(TransferRequest.balanced(
                        dealId,
                        IdempotencyKey.deposit("tx-page-" + i),
                        List.of(
                                new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                                new Leg(AccountId.escrow(dealId),
                                        EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                        null));
            }

            CursorPage<LedgerEntry> page1 = ledgerService
                    .getEntriesByAccount(externalTon, null, 3);
            assertThat(page1.items()).hasSize(3);
            assertThat(page1.hasMore()).isTrue();

            CursorPage<LedgerEntry> page2 = ledgerService
                    .getEntriesByAccount(
                            externalTon, page1.nextCursor(), 3);
            assertThat(page2.items()).hasSize(3);
            assertThat(page2.hasMore()).isTrue();

            CursorPage<LedgerEntry> page3 = ledgerService
                    .getEntriesByAccount(
                            externalTon, page2.nextCursor(), 3);
            assertThat(page3.items()).hasSize(1);
            assertThat(page3.hasMore()).isFalse();

            assertThat(page1.items().getFirst().id())
                    .isGreaterThan(page2.items().getFirst().id());
            assertThat(page2.items().getFirst().id())
                    .isGreaterThan(page3.items().getFirst().id());
        }
    }

    @Nested
    @DisplayName("Cache behavior")
    class CacheBehavior {

        @Test
        @DisplayName("Should evict balance cache after successful transfer")
        void cacheEvictedAfterTransfer() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);

            balanceCache.put(escrow, 0L);
            assertThat(balanceCache.get(escrow)).hasValue(0L);

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-cache-1"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            assertThat(balanceCache.get(escrow))
                    .as("Cache should be evicted after transfer")
                    .isEmpty();

            assertThat(ledgerService.getBalance(escrow))
                    .isEqualTo(ONE_TON);
            assertThat(balanceCache.get(escrow))
                    .as("Cache should be populated after getBalance")
                    .hasValue(ONE_TON);
        }

        @Test
        @DisplayName("Should read balance from cache when available")
        void cacheHit() {
            AccountId account = AccountId.escrow(DealId.generate());
            balanceCache.put(account, 42_000L);

            long balance = ledgerService.getBalance(account);

            assertThat(balance).isEqualTo(42_000L);
        }

        @Test
        @DisplayName("Should fallback to DB on cache miss")
        void cacheMissDbFallback() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-fallback"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            balanceCache.clear();

            long balance = ledgerService.getBalance(escrow);
            assertThat(balance).isEqualTo(ONE_TON);
            assertThat(balanceCache.get(escrow)).hasValue(ONE_TON);
        }
    }

    // --- Test infrastructure ---

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
            return new JooqLedgerRepository(dsl);
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
            return new MetricsFacade(new SimpleMeterRegistry());
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
