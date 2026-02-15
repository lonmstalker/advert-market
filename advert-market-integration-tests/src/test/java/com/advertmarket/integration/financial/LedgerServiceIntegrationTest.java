package com.advertmarket.integration.financial;

import static com.advertmarket.db.generated.tables.AccountBalances.ACCOUNT_BALANCES;
import static com.advertmarket.db.generated.tables.LedgerEntries.LEDGER_ENTRIES;
import static com.advertmarket.db.generated.tables.LedgerIdempotencyKeys.LEDGER_IDEMPOTENCY_KEYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jooq.impl.DSL.sum;

import com.advertmarket.financial.api.model.LedgerEntry;
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
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.util.IdempotencyKey;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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

    @AfterEach
    void doubleEntryInvariant() {
        var totalDebit = sum(LEDGER_ENTRIES.DEBIT_NANO);
        var totalCredit = sum(LEDGER_ENTRIES.CREDIT_NANO);
        var totals = dsl.select(totalDebit, totalCredit)
                .from(LEDGER_ENTRIES)
                .fetchOne();

        BigDecimal debit = totals != null && totals.get(totalDebit) != null
                ? totals.get(totalDebit)
                : BigDecimal.ZERO;
        BigDecimal credit = totals != null && totals.get(totalCredit) != null
                ? totals.get(totalCredit)
                : BigDecimal.ZERO;

        assertThat(debit)
                .as("Global ledger invariant: SUM(debit_nano) must equal SUM(credit_nano)")
                .isEqualTo(credit);
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
    @DisplayName("Transfer validation")
    class TransferValidation {

        @Test
        @DisplayName("Should throw LEDGER_INCONSISTENCY for unbalanced transfer and roll back")
        void unbalancedTransfer_shouldThrowLedgerInconsistency_andRollback() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);
            IdempotencyKey key = IdempotencyKey.deposit("tx-unbalanced");

            TransferRequest request = new TransferRequest(
                    dealId,
                    key,
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(1_000L), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(999L), Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("LEDGER_INCONSISTENCY"));

            assertThat(dsl.fetchCount(LEDGER_IDEMPOTENCY_KEYS,
                    LEDGER_IDEMPOTENCY_KEYS.IDEMPOTENCY_KEY.eq(key.value())))
                    .isZero();
            assertThat(dsl.fetchCount(LEDGER_ENTRIES)).isZero();
            assertThat(dsl.fetchCount(ACCOUNT_BALANCES)).isZero();
        }

        @Test
        @DisplayName("Should throw LEDGER_INCONSISTENCY on overflow and roll back")
        void overflowInValidation_shouldThrowLedgerInconsistency_andRollback() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);
            IdempotencyKey key = IdempotencyKey.deposit("tx-overflow-validation");

            TransferRequest request = new TransferRequest(
                    dealId,
                    key,
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(Long.MAX_VALUE), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(Long.MAX_VALUE), Leg.Side.DEBIT),
                            new Leg(AccountId.platformTreasury(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(1L), Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("LEDGER_INCONSISTENCY"));

            assertThat(dsl.fetchCount(LEDGER_IDEMPOTENCY_KEYS,
                    LEDGER_IDEMPOTENCY_KEYS.IDEMPOTENCY_KEY.eq(key.value())))
                    .isZero();
            assertThat(dsl.fetchCount(LEDGER_ENTRIES)).isZero();
            assertThat(dsl.fetchCount(ACCOUNT_BALANCES)).isZero();
        }
    }

    @Nested
    @DisplayName("Transactional atomicity")
    class TransactionalAtomicity {

        @Test
        @DisplayName("Should roll back when entries insert fails (description too long)")
        void descriptionTooLong_shouldRollbackBalancesAndNotInsertEntries() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);
            IdempotencyKey key = IdempotencyKey.deposit("tx-desc-too-long");
            String description = "x".repeat(501);

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    key,
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    description);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DataAccessException.class);

            assertThat(dsl.fetchCount(LEDGER_IDEMPOTENCY_KEYS,
                    LEDGER_IDEMPOTENCY_KEYS.IDEMPOTENCY_KEY.eq(key.value())))
                    .isZero();
            assertThat(dsl.fetchCount(LEDGER_ENTRIES)).isZero();
            assertThat(dsl.fetchCount(ACCOUNT_BALANCES)).isZero();
        }

        @Test
        @DisplayName("Should roll back when balance upsert fails (account_id too long)")
        void accountIdTooLong_shouldRollbackAndNotInsertEntries() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId tooLongAccount = new AccountId(
                    "OVERPAYMENT:" + "X".repeat(200));
            IdempotencyKey key = IdempotencyKey.deposit("tx-account-too-long");

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    key,
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON / 2),
                                    Leg.Side.CREDIT),
                            new Leg(tooLongAccount, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON / 2),
                                    Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DataAccessException.class);

            assertThat(dsl.fetchCount(LEDGER_IDEMPOTENCY_KEYS,
                    LEDGER_IDEMPOTENCY_KEYS.IDEMPOTENCY_KEY.eq(key.value())))
                    .isZero();
            assertThat(dsl.fetchCount(LEDGER_ENTRIES)).isZero();
            assertThat(dsl.fetchCount(ACCOUNT_BALANCES)).isZero();
        }

        @Test
        @DisplayName("Should leave no state when idempotency key violates DB length constraint")
        void idempotencyKeyTooLong_shouldFailAndLeaveNoState() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);
            IdempotencyKey key = new IdempotencyKey("X".repeat(201));

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    key,
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DataAccessException.class);

            assertThat(dsl.fetchCount(LEDGER_IDEMPOTENCY_KEYS,
                    LEDGER_IDEMPOTENCY_KEYS.IDEMPOTENCY_KEY.eq(key.value())))
                    .isZero();
            assertThat(dsl.fetchCount(LEDGER_ENTRIES)).isZero();
            assertThat(dsl.fetchCount(ACCOUNT_BALANCES)).isZero();
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
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
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
	                        } catch (InterruptedException e) {
	                            Thread.currentThread().interrupt();
	                            throw new IllegalStateException(
	                                    "Interrupted while awaiting latch", e);
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
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
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

    @Nested
    @DisplayName("Transfer with null dealId")
    class TransferWithNullDealId {

        @Test
        @DisplayName("Should record transfer with dealId=null")
        void nullDealId() {
            UUID txRef = ledgerService.transfer(TransferRequest.balanced(
                    null,
                    IdempotencyKey.fee("tx-null-deal"),
                    List.of(
                            new Leg(AccountId.networkFees(),
                                    EntryType.NETWORK_FEE,
                                    Money.ofNano(50_000L), Leg.Side.DEBIT),
                            new Leg(AccountId.externalTon(),
                                    EntryType.NETWORK_FEE,
                                    Money.ofNano(50_000L), Leg.Side.CREDIT)),
                    null));

            assertThat(txRef).isNotNull();

            var entries = dsl.selectFrom(LEDGER_ENTRIES)
                    .where(LEDGER_ENTRIES.TX_REF.eq(txRef))
                    .fetch();
            assertThat(entries).hasSize(2);
            entries.forEach(r ->
                    assertThat(r.get(LEDGER_ENTRIES.DEAL_ID)).isNull());
        }

        @Test
        @DisplayName("Should save description on null-deal transfer")
        void descriptionSaved() {
            UUID txRef = ledgerService.transfer(TransferRequest.balanced(
                    null,
                    IdempotencyKey.fee("tx-null-desc"),
                    List.of(
                            new Leg(AccountId.networkFees(),
                                    EntryType.NETWORK_FEE,
                                    Money.ofNano(10_000L), Leg.Side.DEBIT),
                            new Leg(AccountId.externalTon(),
                                    EntryType.NETWORK_FEE,
                                    Money.ofNano(10_000L), Leg.Side.CREDIT)),
                    "Gas fee for payout"));

            String desc = dsl.select(LEDGER_ENTRIES.DESCRIPTION)
                    .from(LEDGER_ENTRIES)
                    .where(LEDGER_ENTRIES.TX_REF.eq(txRef))
                    .limit(1)
                    .fetchOne(LEDGER_ENTRIES.DESCRIPTION);
            assertThat(desc).isEqualTo("Gas fee for payout");
        }

        @Test
        @DisplayName("Should save null description on null-deal transfer")
        void nullDescription() {
            UUID txRef = ledgerService.transfer(TransferRequest.balanced(
                    null,
                    IdempotencyKey.fee("tx-null-both"),
                    List.of(
                            new Leg(AccountId.networkFees(),
                                    EntryType.NETWORK_FEE,
                                    Money.ofNano(10_000L), Leg.Side.DEBIT),
                            new Leg(AccountId.externalTon(),
                                    EntryType.NETWORK_FEE,
                                    Money.ofNano(10_000L), Leg.Side.CREDIT)),
                    null));

            String desc = dsl.select(LEDGER_ENTRIES.DESCRIPTION)
                    .from(LEDGER_ENTRIES)
                    .where(LEDGER_ENTRIES.TX_REF.eq(txRef))
                    .limit(1)
                    .fetchOne(LEDGER_ENTRIES.DESCRIPTION);
            assertThat(desc).isNull();
        }
    }

    @Nested
    @DisplayName("Idempotency edge cases")
    class IdempotencyEdgeCases {

        @Test
        @DisplayName("Should throw LEDGER_INCONSISTENCY for orphan idempotency key")
        void orphanIdempotencyKey() {
            // Simulate orphan: key inserted manually, no entries
            dsl.insertInto(
                            com.advertmarket.db.generated.tables
                                    .LedgerIdempotencyKeys
                                    .LEDGER_IDEMPOTENCY_KEYS)
                    .set(com.advertmarket.db.generated.tables
                                    .LedgerIdempotencyKeys
                                    .LEDGER_IDEMPOTENCY_KEYS
                                    .IDEMPOTENCY_KEY,
                            "deposit:orphan-key")
                    .execute();

            DealId dealId = DealId.generate();
            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("orphan-key"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.escrow(dealId),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("LEDGER_INCONSISTENCY"));
        }

        @Test
        @DisplayName("Should accept different idempotency keys for same deal")
        void differentKeysForSameDeal() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId ownerPending = AccountId.ownerPending(new UserId(10L));

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-multi-key"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            UUID releaseTxRef = ledgerService.transfer(
                    TransferRequest.balanced(
                            dealId,
                            IdempotencyKey.release(dealId),
                            List.of(
                                    new Leg(escrow, EntryType.ESCROW_RELEASE,
                                            Money.ofNano(ONE_TON),
                                            Leg.Side.DEBIT),
                                    new Leg(ownerPending,
                                            EntryType.OWNER_PAYOUT,
                                            Money.ofNano(ONE_TON),
                                            Leg.Side.CREDIT)),
                            null));

            assertThat(releaseTxRef).isNotNull();
            assertThat(ledgerService.getBalance(escrow)).isZero();
        }
    }

    @Nested
    @DisplayName("Balance check edge cases")
    class BalanceCheckEdgeCases {

        @Test
        @DisplayName("Should succeed on exact debit (balance == amount)")
        void exactDebit() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId ownerPending = AccountId.ownerPending(new UserId(50L));

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-exact"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
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
        }

        @Test
        @DisplayName("Should fail on off-by-one debit (balance + 1)")
        void offByOne() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-obo"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            TransferRequest overDebit = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(ONE_TON + 1),
                                    Leg.Side.DEBIT),
                            new Leg(AccountId.ownerPending(new UserId(51L)),
                                    EntryType.OWNER_PAYOUT,
                                    Money.ofNano(ONE_TON + 1),
                                    Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(overDebit))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INSUFFICIENT_BALANCE"));
        }

        @Test
        @DisplayName("Should allow negative balance for NETWORK_FEES")
        void networkFeesNegative() {
            ledgerService.transfer(TransferRequest.balanced(
                    null,
                    IdempotencyKey.fee("tx-nf-neg"),
                    List.of(
                            new Leg(AccountId.networkFees(),
                                    EntryType.NETWORK_FEE,
                                    Money.ofNano(50_000L), Leg.Side.DEBIT),
                            new Leg(AccountId.externalTon(),
                                    EntryType.NETWORK_FEE,
                                    Money.ofNano(50_000L), Leg.Side.CREDIT)),
                    null));

            assertThat(ledgerService.getBalance(AccountId.networkFees()))
                    .isEqualTo(-50_000L);
        }

        @Test
        @DisplayName("Should allow balance updates for DUST_WRITEOFF (allow-negative account type)")
        void dustWriteoffBalanceUpdates() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx-dust-seed"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(100L), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(100L), Leg.Side.CREDIT)),
                    null));

            ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.DUST_WRITEOFF,
                                    Money.ofNano(100L), Leg.Side.DEBIT),
                            new Leg(AccountId.dustWriteoff(),
                                    EntryType.DUST_WRITEOFF,
                                    Money.ofNano(100L), Leg.Side.CREDIT)),
                    null));

            assertThat(ledgerService.getBalance(AccountId.dustWriteoff()))
                    .isEqualTo(100L);
        }

        @Test
        @DisplayName("Should allow negative balance for DUST_WRITEOFF when debited")
        void dustWriteoffDebitAllowsNegative() {
            ledgerService.transfer(TransferRequest.balanced(
                    null,
                    new IdempotencyKey("dust-debit"),
                    List.of(
                            new Leg(AccountId.dustWriteoff(),
                                    EntryType.DUST_WRITEOFF,
                                    Money.ofNano(5L), Leg.Side.DEBIT),
                            new Leg(AccountId.externalTon(),
                                    EntryType.DUST_WRITEOFF,
                                    Money.ofNano(5L), Leg.Side.CREDIT)),
                    null));

            assertThat(ledgerService.getBalance(AccountId.dustWriteoff()))
                    .isEqualTo(-5L);
        }

        @Test
        @DisplayName("Should require non-negative for PLATFORM_TREASURY")
        void platformTreasuryNonNeg() {
            DealId dealId = DealId.generate();
            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(AccountId.platformTreasury(),
                                    EntryType.COMMISSION_SWEEP,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.externalTon(),
                                    EntryType.COMMISSION_SWEEP,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INSUFFICIENT_BALANCE"));
        }

        @Test
        @DisplayName("Should require non-negative for OWNER_PENDING")
        void ownerPendingNonNeg() {
            DealId dealId = DealId.generate();
            AccountId ownerPending = AccountId.ownerPending(new UserId(60L));

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.payout(dealId),
                    List.of(
                            new Leg(ownerPending,
                                    EntryType.OWNER_WITHDRAWAL,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.externalTon(),
                                    EntryType.OWNER_WITHDRAWAL,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INSUFFICIENT_BALANCE"));
        }

        @Test
        @DisplayName("Should require non-negative for COMMISSION")
        void commissionNonNeg() {
            DealId dealId = DealId.generate();
            AccountId commission = AccountId.commission(dealId);

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.sweep("2026-01-15", commission),
                    List.of(
                            new Leg(commission,
                                    EntryType.COMMISSION_SWEEP,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.platformTreasury(),
                                    EntryType.COMMISSION_SWEEP,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INSUFFICIENT_BALANCE"));
        }

        @Test
        @DisplayName("Should require non-negative for OVERPAYMENT")
        void overpaymentNonNeg() {
            DealId dealId = DealId.generate();
            AccountId overpayment = AccountId.overpayment(dealId);

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.overpaymentRefund(dealId, "tx-op-ref"),
                    List.of(
                            new Leg(overpayment,
                                    EntryType.OVERPAYMENT_REFUND,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.externalTon(),
                                    EntryType.OVERPAYMENT_REFUND,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INSUFFICIENT_BALANCE"));
        }

        @Test
        @DisplayName("Should require non-negative for PARTIAL_DEPOSIT")
        void partialDepositNonNeg() {
            DealId dealId = DealId.generate();
            AccountId partialDeposit = AccountId.partialDeposit(dealId);

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.promote(dealId),
                    List.of(
                            new Leg(partialDeposit,
                                    EntryType.PARTIAL_DEPOSIT_PROMOTE,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.escrow(dealId),
                                    EntryType.PARTIAL_DEPOSIT_PROMOTE,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INSUFFICIENT_BALANCE"));
        }

        @Test
        @DisplayName("Should require non-negative for LATE_DEPOSIT")
        void lateDepositNonNeg() {
            DealId dealId = DealId.generate();
            AccountId lateDeposit = AccountId.lateDeposit(dealId);
            IdempotencyKey key = IdempotencyKey.lateDepositRefund(
                    dealId, "tx-late-empty");

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    key,
                    List.of(
                            new Leg(lateDeposit,
                                    EntryType.LATE_DEPOSIT_REFUND,
                                    Money.ofNano(ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(AccountId.externalTon(),
                                    EntryType.LATE_DEPOSIT_REFUND,
                                    Money.ofNano(ONE_TON),
                                    Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INSUFFICIENT_BALANCE"));

            assertThat(dsl.fetchCount(LEDGER_IDEMPOTENCY_KEYS,
                    LEDGER_IDEMPOTENCY_KEYS.IDEMPOTENCY_KEY.eq(key.value())))
                    .isZero();
            assertThat(dsl.fetchCount(LEDGER_ENTRIES)).isZero();
            assertThat(dsl.fetchCount(ACCOUNT_BALANCES)).isZero();
        }
    }

    @Nested
    @DisplayName("Business flow scenarios")
    class BusinessFlowScenarios {

        @Test
        @DisplayName("Full lifecycle: deposit -> release (owner + commission)")
        void fullLifecycle() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId ownerPending = AccountId.ownerPending(new UserId(70L));
            AccountId commission = AccountId.commission(dealId);

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-lc-dep"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(10 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(10 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    "Deposit"));

            long commissionAmount = ONE_TON;
            long ownerAmount = 9 * ONE_TON;
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(10 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(ownerPending, EntryType.OWNER_PAYOUT,
                                    Money.ofNano(ownerAmount),
                                    Leg.Side.CREDIT),
                            new Leg(commission,
                                    EntryType.PLATFORM_COMMISSION,
                                    Money.ofNano(commissionAmount),
                                    Leg.Side.CREDIT)),
                    "Release"));

            assertThat(ledgerService.getBalance(escrow)).isZero();
            assertThat(ledgerService.getBalance(ownerPending))
                    .isEqualTo(ownerAmount);
            assertThat(ledgerService.getBalance(commission))
                    .isEqualTo(commissionAmount);
        }

        @Test
        @DisplayName("Full refund: deposit -> escrow_refund (net zero)")
        void fullRefund() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-refund-dep"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(5 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(5 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.refund(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_REFUND,
                                    Money.ofNano(5 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(externalTon, EntryType.ESCROW_REFUND,
                                    Money.ofNano(5 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    "Full refund"));

            assertThat(ledgerService.getBalance(escrow)).isZero();
            assertThat(ledgerService.getBalance(externalTon)).isZero();
        }

        @Test
        @DisplayName("Partial refund: escrow -> (external + owner + commission)")
        void partialRefund() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId ownerPending = AccountId.ownerPending(new UserId(777L));
            AccountId commission = AccountId.commission(dealId);

            long amount = 10 * ONE_TON;
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-pr-dep"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(amount), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(amount), Leg.Side.CREDIT)),
                    null));

            long refundAmount = 4 * ONE_TON;
            long ownerAmount = 5 * ONE_TON;
            long commissionAmount = ONE_TON;

            UUID txRef = ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.partialRefund(dealId),
                    List.of(
                            new Leg(escrow, EntryType.PARTIAL_REFUND,
                                    Money.ofNano(amount), Leg.Side.DEBIT),
                            new Leg(externalTon, EntryType.PARTIAL_REFUND,
                                    Money.ofNano(refundAmount), Leg.Side.CREDIT),
                            new Leg(ownerPending, EntryType.PARTIAL_REFUND,
                                    Money.ofNano(ownerAmount), Leg.Side.CREDIT),
                            new Leg(commission, EntryType.PARTIAL_REFUND,
                                    Money.ofNano(commissionAmount), Leg.Side.CREDIT)),
                    "Partial refund split"));

            assertThat(txRef).isNotNull();
            assertThat(ledgerService.getBalance(escrow)).isZero();
            assertThat(ledgerService.getBalance(externalTon))
                    .isEqualTo(-(amount - refundAmount));
            assertThat(ledgerService.getBalance(ownerPending))
                    .isEqualTo(ownerAmount);
            assertThat(ledgerService.getBalance(commission))
                    .isEqualTo(commissionAmount);

            assertThat(dsl.fetchCount(
                    LEDGER_ENTRIES,
                    LEDGER_ENTRIES.TX_REF.eq(txRef)
                            .and(LEDGER_ENTRIES.ENTRY_TYPE.eq(
                                    EntryType.PARTIAL_REFUND.name()))))
                    .isEqualTo(4);
        }

        @Test
        @DisplayName("Refund with fee: escrow_refund + network_fee_refund tagging")
        void refundWithFee() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId networkFees = AccountId.networkFees();

            long amount = 5 * ONE_TON;
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-fee-refund-dep"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(amount), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(amount), Leg.Side.CREDIT)),
                    null));

            long fee = 50_000L;
            long netRefund = amount - fee;

            UUID txRef = ledgerService.transfer(TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.refund(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_REFUND,
                                    Money.ofNano(amount), Leg.Side.DEBIT),
                            new Leg(externalTon, EntryType.ESCROW_REFUND,
                                    Money.ofNano(netRefund), Leg.Side.CREDIT),
                            new Leg(networkFees,
                                    EntryType.NETWORK_FEE_REFUND,
                                    Money.ofNano(fee), Leg.Side.CREDIT)),
                    "Refund with fee"));

            assertThat(txRef).isNotNull();
            assertThat(ledgerService.getBalance(escrow)).isZero();
            assertThat(ledgerService.getBalance(externalTon))
                    .isEqualTo(-fee);
            assertThat(ledgerService.getBalance(networkFees))
                    .isEqualTo(fee);

            assertThat(dsl.fetchCount(
                    LEDGER_ENTRIES,
                    LEDGER_ENTRIES.TX_REF.eq(txRef)
                            .and(LEDGER_ENTRIES.ENTRY_TYPE.eq(
                                    EntryType.NETWORK_FEE_REFUND.name()))))
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Partial deposit -> promote to escrow")
        void partialDepositPromote() {
            DealId dealId = DealId.generate();
            AccountId partialDep = AccountId.partialDeposit(dealId);
            AccountId escrow = AccountId.escrow(dealId);
            AccountId externalTon = AccountId.externalTon();

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.partialDeposit(dealId, "tx-pd-1"),
                    List.of(
                            new Leg(externalTon, EntryType.PARTIAL_DEPOSIT,
                                    Money.ofNano(3 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(partialDep, EntryType.PARTIAL_DEPOSIT,
                                    Money.ofNano(3 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.promote(dealId),
                    List.of(
                            new Leg(partialDep,
                                    EntryType.PARTIAL_DEPOSIT_PROMOTE,
                                    Money.ofNano(3 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(escrow,
                                    EntryType.PARTIAL_DEPOSIT_PROMOTE,
                                    Money.ofNano(3 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));

            assertThat(ledgerService.getBalance(partialDep)).isZero();
            assertThat(ledgerService.getBalance(escrow))
                    .isEqualTo(3 * ONE_TON);
        }

        @Test
        @DisplayName("Overpayment -> overpayment_refund")
        void overpaymentRefund() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId overpayment = AccountId.overpayment(dealId);

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-op-dep"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(2 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(overpayment, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(2 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.overpaymentRefund(dealId, "tx-op-ref"),
                    List.of(
                            new Leg(overpayment,
                                    EntryType.OVERPAYMENT_REFUND,
                                    Money.ofNano(2 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(externalTon,
                                    EntryType.OVERPAYMENT_REFUND,
                                    Money.ofNano(2 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));

            assertThat(ledgerService.getBalance(overpayment)).isZero();
        }

        @Test
        @DisplayName("Late deposit -> auto-refund")
        void lateDepositAutoRefund() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId lateDep = AccountId.lateDeposit(dealId);

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-late-dep"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(lateDep, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.lateDepositRefund(dealId, "tx-late-ref"),
                    List.of(
                            new Leg(lateDep,
                                    EntryType.LATE_DEPOSIT_REFUND,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(externalTon,
                                    EntryType.LATE_DEPOSIT_REFUND,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            assertThat(ledgerService.getBalance(lateDep)).isZero();
        }

        @Test
        @DisplayName("Commission sweep: commission -> treasury")
        void commissionSweep() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId commission = AccountId.commission(dealId);
            AccountId treasury = AccountId.platformTreasury();

            // Seed escrow
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-sweep-dep"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(10 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(10 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));
            // Release with commission
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(10 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(AccountId.ownerPending(new UserId(71L)),
                                    EntryType.OWNER_PAYOUT,
                                    Money.ofNano(9 * ONE_TON),
                                    Leg.Side.CREDIT),
                            new Leg(commission,
                                    EntryType.PLATFORM_COMMISSION,
                                    Money.ofNano(ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));
            // Sweep commission to treasury
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.sweep("2026-02-15", commission),
                    List.of(
                            new Leg(commission,
                                    EntryType.COMMISSION_SWEEP,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(treasury,
                                    EntryType.COMMISSION_SWEEP,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            assertThat(ledgerService.getBalance(commission)).isZero();
            assertThat(ledgerService.getBalance(treasury))
                    .isEqualTo(ONE_TON);
        }

        @Test
        @DisplayName("Owner withdrawal: owner_pending -> external + network_fee")
        void ownerWithdrawal() {
            DealId dealId = DealId.generate();
            UserId userId = new UserId(72L);
            AccountId ownerPending = AccountId.ownerPending(userId);
            AccountId escrow = AccountId.escrow(dealId);
            AccountId externalTon = AccountId.externalTon();

            // Seed owner
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-wd-dep"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(5 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(5 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(5 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(ownerPending, EntryType.OWNER_PAYOUT,
                                    Money.ofNano(5 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));

            // Withdrawal with fee
            long fee = 50_000L;
            long netAmount = 5 * ONE_TON - fee;
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.withdrawal(userId, "2026-02-15T12:00"),
                    List.of(
                            new Leg(ownerPending,
                                    EntryType.OWNER_WITHDRAWAL,
                                    Money.ofNano(5 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(externalTon,
                                    EntryType.OWNER_WITHDRAWAL,
                                    Money.ofNano(netAmount),
                                    Leg.Side.CREDIT),
                            new Leg(AccountId.networkFees(),
                                    EntryType.NETWORK_FEE,
                                    Money.ofNano(fee), Leg.Side.CREDIT)),
                    "Owner withdrawal"));

            assertThat(ledgerService.getBalance(ownerPending)).isZero();
        }

        @Test
        @DisplayName("Reversal: reverses previous transfer")
        void reversal() {
            DealId dealId = DealId.generate();
            AccountId externalTon = AccountId.externalTon();
            AccountId escrow = AccountId.escrow(dealId);

            UUID originalTxRef = ledgerService.transfer(
                    TransferRequest.balanced(dealId,
                            IdempotencyKey.deposit("tx-rev-orig"),
                            List.of(
                                    new Leg(externalTon,
                                            EntryType.ESCROW_DEPOSIT,
                                            Money.ofNano(ONE_TON),
                                            Leg.Side.DEBIT),
                                    new Leg(escrow,
                                            EntryType.ESCROW_DEPOSIT,
                                            Money.ofNano(ONE_TON),
                                            Leg.Side.CREDIT)),
                            null));

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.reversal(originalTxRef.toString()),
                    List.of(
                            new Leg(escrow, EntryType.REVERSAL,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(externalTon, EntryType.REVERSAL,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    "Reversal"));

            assertThat(ledgerService.getBalance(escrow)).isZero();
            assertThat(ledgerService.getBalance(externalTon)).isZero();
        }

        @Test
        @DisplayName("Fee adjustment: corrects commission")
        void feeAdjustment() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId commission = AccountId.commission(dealId);
            AccountId ownerPending = AccountId.ownerPending(new UserId(73L));

            // Initial deposit + release with wrong commission
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-fa-dep"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(10 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(10 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(10 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(ownerPending, EntryType.OWNER_PAYOUT,
                                    Money.ofNano(8 * ONE_TON),
                                    Leg.Side.CREDIT),
                            new Leg(commission,
                                    EntryType.PLATFORM_COMMISSION,
                                    Money.ofNano(2 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));

            // Fee adjustment: move 0.5 TON from commission back to owner
            long adjustment = ONE_TON / 2;
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    new IdempotencyKey("fee-adjust:" + dealId.value()),
                    List.of(
                            new Leg(commission, EntryType.FEE_ADJUSTMENT,
                                    Money.ofNano(adjustment),
                                    Leg.Side.DEBIT),
                            new Leg(ownerPending, EntryType.FEE_ADJUSTMENT,
                                    Money.ofNano(adjustment),
                                    Leg.Side.CREDIT)),
                    "Fee correction"));

            assertThat(ledgerService.getBalance(commission))
                    .isEqualTo(2 * ONE_TON - adjustment);
            assertThat(ledgerService.getBalance(ownerPending))
                    .isEqualTo(8 * ONE_TON + adjustment);
        }

        @Test
        @DisplayName("Dust writeoff: escrow -> dust_writeoff")
        void dustWriteoff() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-dust-dep"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(5L), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(5L), Leg.Side.CREDIT)),
                    null));

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    new IdempotencyKey("dust:" + dealId.value()),
                    List.of(
                            new Leg(escrow, EntryType.DUST_WRITEOFF,
                                    Money.ofNano(5L), Leg.Side.DEBIT),
                            new Leg(AccountId.dustWriteoff(),
                                    EntryType.DUST_WRITEOFF,
                                    Money.ofNano(5L), Leg.Side.CREDIT)),
                    "Dust writeoff"));

            assertThat(ledgerService.getBalance(escrow)).isZero();
        }
    }

    @Nested
    @DisplayName("Query edge cases")
    class QueryEdgeCases {

        @Test
        @DisplayName("Should throw INVALID_CURSOR for non-numeric cursor")
        void invalidCursor() {
            assertThatThrownBy(() ->
                    ledgerService.getEntriesByAccount(
                            AccountId.externalTon(), "not-a-number", 10))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INVALID_CURSOR"));
        }

        @Test
        @DisplayName("Should throw INVALID_CURSOR for numeric cursor overflow")
        void cursorOverflow() {
            assertThatThrownBy(() ->
                    ledgerService.getEntriesByAccount(
                            AccountId.externalTon(),
                            "9223372036854775808",
                            10))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INVALID_CURSOR"));
        }

        @Test
        @DisplayName("Should use default page size (50) for negative limit")
        void negativeLimitUsesDefaultPageSize() {
            AccountId account = AccountId.externalTon();
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);
            for (int i = 0; i < 60; i++) {
                ledgerService.transfer(TransferRequest.balanced(dealId,
                        IdempotencyKey.deposit("tx-neg-lim-" + i),
                        List.of(
                                new Leg(account, EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.DEBIT),
                                new Leg(escrow,
                                        EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.CREDIT)),
                        null));
            }

            CursorPage<LedgerEntry> page = ledgerService
                    .getEntriesByAccount(account, null, -1);

            assertThat(page.items()).hasSize(50);
            assertThat(page.hasMore()).isTrue();
            assertThat(page.nextCursor()).isNotNull();
        }

        @Test
        @DisplayName("Should use default page size (50) when limit is 0")
        void limitZeroUsesDefaultPageSize() {
            AccountId account = AccountId.externalTon();
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);
            for (int i = 0; i < 60; i++) {
                ledgerService.transfer(TransferRequest.balanced(dealId,
                        IdempotencyKey.deposit("tx-zero-lim-" + i),
                        List.of(
                                new Leg(account, EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.DEBIT),
                                new Leg(escrow,
                                        EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.CREDIT)),
                        null));
            }

            CursorPage<LedgerEntry> page = ledgerService
                    .getEntriesByAccount(account, null, 0);

            assertThat(page.items()).hasSize(50);
            assertThat(page.hasMore()).isTrue();
            assertThat(page.nextCursor()).isNotNull();
        }

        @Test
        @DisplayName("Should cap limit at MAX_PAGE_SIZE (200)")
        void hugeLimitIsCappedAtMaxPageSize() {
            AccountId account = AccountId.externalTon();
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);
            for (int i = 0; i < 205; i++) {
                ledgerService.transfer(TransferRequest.balanced(dealId,
                        IdempotencyKey.deposit("tx-huge-lim-" + i),
                        List.of(
                                new Leg(account, EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.DEBIT),
                                new Leg(escrow,
                                        EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.CREDIT)),
                        null));
            }

            CursorPage<LedgerEntry> page = ledgerService
                    .getEntriesByAccount(account, null, 1_000_000);

            assertThat(page.items()).hasSize(200);
            assertThat(page.hasMore()).isTrue();
            assertThat(page.nextCursor()).isNotNull();
        }

        @Test
        @DisplayName("Should return hasMore=false when entries fit in one page")
        void fitsOnePage() {
            AccountId account = AccountId.externalTon();
            DealId dealId = DealId.generate();
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-fits"),
                    List.of(
                            new Leg(account, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.escrow(dealId),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            CursorPage<LedgerEntry> page = ledgerService
                    .getEntriesByAccount(account, null, 10);

            assertThat(page.hasMore()).isFalse();
        }

        @Test
        @DisplayName("Should have no overlap between pages")
        void noOverlap() {
            AccountId account = AccountId.externalTon();
            for (int i = 0; i < 7; i++) {
                DealId dealId = DealId.generate();
                ledgerService.transfer(TransferRequest.balanced(dealId,
                        IdempotencyKey.deposit("tx-overlap-" + i),
                        List.of(
                                new Leg(account, EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.DEBIT),
                                new Leg(AccountId.escrow(dealId),
                                        EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.CREDIT)),
                        null));
            }

            CursorPage<LedgerEntry> page1 = ledgerService
                    .getEntriesByAccount(account, null, 3);
            CursorPage<LedgerEntry> page2 = ledgerService
                    .getEntriesByAccount(
                            account, page1.nextCursor(), 3);
            CursorPage<LedgerEntry> page3 = ledgerService
                    .getEntriesByAccount(
                            account, page2.nextCursor(), 3);

            Set<Long> allIds = new HashSet<>();
            page1.items().forEach(e -> allIds.add(e.id()));
            page2.items().forEach(e -> allIds.add(e.id()));
            page3.items().forEach(e -> allIds.add(e.id()));

            assertThat(allIds).hasSize(
                    page1.items().size()
                            + page2.items().size()
                            + page3.items().size());
        }

        @Test
        @DisplayName("Should order entries by ID DESC")
        void orderById() {
            AccountId account = AccountId.externalTon();
            for (int i = 0; i < 5; i++) {
                DealId dealId = DealId.generate();
                ledgerService.transfer(TransferRequest.balanced(dealId,
                        IdempotencyKey.deposit("tx-order-" + i),
                        List.of(
                                new Leg(account, EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.DEBIT),
                                new Leg(AccountId.escrow(dealId),
                                        EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.CREDIT)),
                        null));
            }

            CursorPage<LedgerEntry> page = ledgerService
                    .getEntriesByAccount(account, null, 10);

            List<Long> ids = page.items().stream()
                    .map(LedgerEntry::id).toList();
            for (int i = 0; i < ids.size() - 1; i++) {
                assertThat(ids.get(i)).isGreaterThan(ids.get(i + 1));
            }
        }

        @Test
        @DisplayName("Should return empty page when cursor past all entries")
        void cursorPastAll() {
            AccountId account = AccountId.externalTon();
            DealId dealId = DealId.generate();
            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-past"),
                    List.of(
                            new Leg(account, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.escrow(dealId),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            CursorPage<LedgerEntry> page = ledgerService
                    .getEntriesByAccount(account, "1", 10);

            assertThat(page.items()).isEmpty();
            assertThat(page.hasMore()).isFalse();
        }
    }

    @Nested
    @DisplayName("Accounting invariants extended")
    class AccountingInvariantExtended {

        @Test
        @DisplayName("Multi-deal stress: global + per-txRef + per-account invariants")
        void multiDealStress() {
            for (int i = 0; i < 5; i++) {
                DealId dealId = DealId.generate();
                AccountId escrow = AccountId.escrow(dealId);
                AccountId ownerPending = AccountId.ownerPending(
                        new UserId(80L + i));
                AccountId commission = AccountId.commission(dealId);

                long amount = (i + 1) * ONE_TON;
                long commFee = amount / 10;
                long ownerAmt = amount - commFee;

                ledgerService.transfer(TransferRequest.balanced(dealId,
                        IdempotencyKey.deposit("tx-stress-" + i),
                        List.of(
                                new Leg(AccountId.externalTon(),
                                        EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(amount),
                                        Leg.Side.DEBIT),
                                new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(amount),
                                        Leg.Side.CREDIT)),
                        null));

                ledgerService.transfer(TransferRequest.balanced(dealId,
                        IdempotencyKey.release(dealId),
                        List.of(
                                new Leg(escrow, EntryType.ESCROW_RELEASE,
                                        Money.ofNano(amount),
                                        Leg.Side.DEBIT),
                                new Leg(ownerPending,
                                        EntryType.OWNER_PAYOUT,
                                        Money.ofNano(ownerAmt),
                                        Leg.Side.CREDIT),
                                new Leg(commission,
                                        EntryType.PLATFORM_COMMISSION,
                                        Money.ofNano(commFee),
                                        Leg.Side.CREDIT)),
                        null));
            }

            // Global invariant: SUM(debit) == SUM(credit)
            var totalDebit = sum(LEDGER_ENTRIES.DEBIT_NANO);
            var totalCredit = sum(LEDGER_ENTRIES.CREDIT_NANO);
            var totals = dsl.select(totalDebit, totalCredit)
                    .from(LEDGER_ENTRIES)
                    .fetchOne();
            assertThat(totals).isNotNull();
            assertThat(totals.get(totalDebit))
                    .isEqualTo(totals.get(totalCredit));

            // Per-txRef invariant
            var debitSum = sum(LEDGER_ENTRIES.DEBIT_NANO);
            var creditSum = sum(LEDGER_ENTRIES.CREDIT_NANO);
            var perTx = dsl.select(
                            LEDGER_ENTRIES.TX_REF, debitSum, creditSum)
                    .from(LEDGER_ENTRIES)
                    .groupBy(LEDGER_ENTRIES.TX_REF)
                    .fetch();
            perTx.forEach(r ->
                    assertThat(r.get(debitSum)).isEqualTo(r.get(creditSum)));

            // Per-account: balance == credits - debits
            var accounts = dsl.select(
                            LEDGER_ENTRIES.ACCOUNT_ID,
                            sum(LEDGER_ENTRIES.CREDIT_NANO),
                            sum(LEDGER_ENTRIES.DEBIT_NANO))
                    .from(LEDGER_ENTRIES)
                    .groupBy(LEDGER_ENTRIES.ACCOUNT_ID)
                    .fetch();
            for (var row : accounts) {
                String accId = row.get(LEDGER_ENTRIES.ACCOUNT_ID);
                long credits = row.value2().longValue();
                long debits = row.value3().longValue();
                long expected = credits - debits;

                Long actual = dsl.select(ACCOUNT_BALANCES.BALANCE_NANO)
                        .from(ACCOUNT_BALANCES)
                        .where(ACCOUNT_BALANCES.ACCOUNT_ID.eq(accId))
                        .fetchOne(ACCOUNT_BALANCES.BALANCE_NANO);
                assertThat(actual)
                        .as("Balance for " + accId)
                        .isEqualTo(expected);
            }
        }

        @Test
        @DisplayName("Version column increments correctly across operations")
        void versionIncrement() {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-ver-1"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            Integer v1 = dsl.select(ACCOUNT_BALANCES.VERSION)
                    .from(ACCOUNT_BALANCES)
                    .where(ACCOUNT_BALANCES.ACCOUNT_ID.eq(escrow.value()))
                    .fetchOne(ACCOUNT_BALANCES.VERSION);
            assertThat(v1).isEqualTo(1);

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.ownerPending(new UserId(90L)),
                                    EntryType.OWNER_PAYOUT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            Integer v2 = dsl.select(ACCOUNT_BALANCES.VERSION)
                    .from(ACCOUNT_BALANCES)
                    .where(ACCOUNT_BALANCES.ACCOUNT_ID.eq(escrow.value()))
                    .fetchOne(ACCOUNT_BALANCES.VERSION);
            assertThat(v2).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Concurrency extended")
    class ConcurrencyExtended {

        @Test
        @DisplayName("Concurrent reads during writes should not throw")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void concurrentReadsAndWrites() throws Exception {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-crw-seed"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(100 * ONE_TON),
                                    Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(100 * ONE_TON),
                                    Leg.Side.CREDIT)),
                    null));

            int threads = 10;
            CountDownLatch latch = new CountDownLatch(1);
            List<Long> balances = java.util.Collections
                    .synchronizedList(new ArrayList<>());
            AtomicInteger exceptions = new AtomicInteger();

            try (ExecutorService executor =
                         Executors.newFixedThreadPool(threads)) {
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    int idx = i;
                    futures.add(executor.submit(() -> {
                        try {
                            latch.await();
                            if (idx % 2 == 0) {
                                balances.add(
                                        ledgerService.getBalance(escrow));
                            } else {
                                DealId newDeal = DealId.generate();
                                ledgerService.transfer(
                                        TransferRequest.balanced(dealId,
                                                IdempotencyKey.deposit(
                                                        "tx-crw-" + idx),
                                                List.of(
                                                        new Leg(AccountId
                                                                .externalTon(),
                                                                EntryType
                                                                .ESCROW_DEPOSIT,
                                                                Money.ofNano(
                                                                        ONE_TON),
                                                                Leg.Side
                                                                        .DEBIT),
                                                        new Leg(escrow,
                                                                EntryType
                                                                .ESCROW_DEPOSIT,
                                                                Money.ofNano(
                                                                        ONE_TON),
                                                                Leg.Side
                                                                        .CREDIT)),
                                                null));
                            }
                        } catch (DomainException | InterruptedException ex) {
                            exceptions.incrementAndGet();
                        }
                    }));
                }
                latch.countDown();
                for (Future<?> f : futures) {
                    f.get();
                }
            }

            assertThat(exceptions.get()).isZero();
            assertThat(ledgerService.getBalance(escrow))
                    .isGreaterThanOrEqualTo(100 * ONE_TON);
        }

        @Test
        @DisplayName("Double-spend race: exactly one of two debits succeeds")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void doubleSpendRace() throws Exception {
            DealId dealId = DealId.generate();
            AccountId escrow = AccountId.escrow(dealId);

            ledgerService.transfer(TransferRequest.balanced(dealId,
                    IdempotencyKey.deposit("tx-ds-seed"),
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)),
                    null));

            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger();
            AtomicInteger failCount = new AtomicInteger();

            try (ExecutorService executor =
                         Executors.newFixedThreadPool(2)) {
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    int idx = i;
                    futures.add(executor.submit(() -> {
                        try {
                            latch.await();
                            ledgerService.transfer(
                                    TransferRequest.balanced(dealId,
                                            IdempotencyKey.release(
                                                    DealId.of(UUID
                                                            .randomUUID())),
                                            List.of(
                                                    new Leg(escrow,
                                                            EntryType
                                                            .ESCROW_RELEASE,
                                                            Money.ofNano(
                                                                    ONE_TON),
                                                            Leg.Side.DEBIT),
                                                    new Leg(AccountId
                                                            .ownerPending(
                                                                    new UserId(
                                                                            91L
                                                                                    + idx)),
                                                            EntryType
                                                            .OWNER_PAYOUT,
                                                            Money.ofNano(
                                                                    ONE_TON),
                                                            Leg.Side
                                                                    .CREDIT)),
                                            null));
                            successCount.incrementAndGet();
                        } catch (DomainException
                                 | InterruptedException ex) {
                            failCount.incrementAndGet();
                        }
                    }));
                }
                latch.countDown();
                for (Future<?> f : futures) {
                    f.get();
                }
            }

            assertThat(successCount.get() + failCount.get()).isEqualTo(2);
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(ledgerService.getBalance(escrow)).isZero();
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
