package com.advertmarket.integration.financial;

import static com.advertmarket.db.generated.tables.TonTransactions.TON_TRANSACTIONS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.db.generated.tables.records.TonTransactionsRecord;
import com.advertmarket.financial.ton.repository.JooqTonTransactionRepository;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.SharedContainers;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests verifying concurrency guarantees for TON deposit processing.
 * PG-only — no Spring context required.
 */
@DisplayName("TON transaction concurrency — PostgreSQL integration")
class TonTransactionConcurrencyIntegrationTest {

    private static DSLContext dsl;
    private static JooqTonTransactionRepository repository;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
        repository = new JooqTonTransactionRepository(dsl);
    }

    @BeforeEach
    void cleanUp() {
        DatabaseSupport.cleanDealTables(dsl);
    }

    @Nested
    @DisplayName("FOR UPDATE SKIP LOCKED")
    class SkipLocked {

        @Test
        @DisplayName("Should skip locked rows when two connections query pending deposits concurrently")
        void secondConnectionSkipsLockedRows() throws Exception {
            // Insert 2 pending IN deposits
            insertPendingDeposit(1_000_000_000L, "EQAddr1");
            insertPendingDeposit(2_000_000_000L, "EQAddr2");

            var latch = new CountDownLatch(1);
            var conn1Results = new AtomicReference<List<TonTransactionsRecord>>();
            var conn2Results = new AtomicReference<List<TonTransactionsRecord>>();

            // Thread 1: open transaction, lock all pending rows, wait for signal
            var thread1 = Thread.ofVirtual().start(() -> {
                try (Connection conn = newConnection()) {
                    conn.setAutoCommit(false);
                    var localDsl = DSL.using(conn, SQLDialect.POSTGRES);
                    var repo = new JooqTonTransactionRepository(localDsl);

                    var result = repo.findPendingDeposits(10);
                    conn1Results.set(result);

                    // Hold the lock until main thread signals
                    latch.await();
                    conn.commit();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Give thread 1 time to acquire locks
            Thread.sleep(200);

            // Thread 2: open separate transaction, query — should get empty list
            try (Connection conn = newConnection()) {
                conn.setAutoCommit(false);
                var localDsl = DSL.using(conn, SQLDialect.POSTGRES);
                var repo = new JooqTonTransactionRepository(localDsl);

                var result = repo.findPendingDeposits(10);
                conn2Results.set(result);
                conn.commit();
            }

            // Release thread 1
            latch.countDown();
            thread1.join(5_000);

            assertThat(conn1Results.get()).hasSize(2);
            assertThat(conn2Results.get()).isEmpty();
        }
    }

    @Nested
    @DisplayName("CAS updateConfirmed")
    class CasUpdateConfirmed {

        @Test
        @DisplayName("Should allow only one of two concurrent updateConfirmed calls to succeed")
        void onlyOneWins() throws Exception {
            long txId = insertPendingDeposit(5_000_000_000L, "EQConcurrent");

            var startLatch = new CountDownLatch(1);
            var result1 = new AtomicBoolean();
            var result2 = new AtomicBoolean();

            var thread1 = Thread.ofVirtual().start(() -> {
                try {
                    startLatch.await();
                    result1.set(repository.updateConfirmed(
                            txId, "hash_t1", 3, 100_000L,
                            OffsetDateTime.now(), 0));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            var thread2 = Thread.ofVirtual().start(() -> {
                try {
                    startLatch.await();
                    result2.set(repository.updateConfirmed(
                            txId, "hash_t2", 3, 200_000L,
                            OffsetDateTime.now(), 0));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Release both threads simultaneously
            startLatch.countDown();
            thread1.join(5_000);
            thread2.join(5_000);

            // Exactly one should succeed
            assertThat(result1.get() ^ result2.get())
                    .as("Exactly one CAS update should succeed")
                    .isTrue();

            // Verify the row has version=1 (only one increment)
            var record = dsl.selectFrom(TON_TRANSACTIONS)
                    .where(TON_TRANSACTIONS.ID.eq(txId))
                    .fetchSingle();
            assertThat(record.getVersion()).isEqualTo(1);
            assertThat(record.getStatus()).isEqualTo("CONFIRMED");
        }
    }

    @Nested
    @DisplayName("incrementRetryCount")
    class IncrementRetryCount {

        @Test
        @DisplayName("Should produce correct count after concurrent increments")
        void concurrentIncrementsAreSerializable() throws Exception {
            long txId = insertPendingDeposit(3_000_000_000L, "EQRetry");
            int concurrency = 5;

            var startLatch = new CountDownLatch(1);
            var threads = new Thread[concurrency];

            for (int i = 0; i < concurrency; i++) {
                threads[i] = Thread.ofVirtual().start(() -> {
                    try {
                        startLatch.await();
                        repository.incrementRetryCount(txId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            startLatch.countDown();
            for (var thread : threads) {
                thread.join(5_000);
            }

            var record = dsl.selectFrom(TON_TRANSACTIONS)
                    .where(TON_TRANSACTIONS.ID.eq(txId))
                    .fetchSingle();
            assertThat(record.getRetryCount()).isEqualTo(concurrency);
        }
    }

    private long insertPendingDeposit(long amountNano, String toAddress) {
        var record = dsl.newRecord(TON_TRANSACTIONS);
        record.setDirection("IN");
        record.setAmountNano(amountNano);
        record.setToAddress(toAddress);
        record.setStatus("PENDING");
        record.setConfirmations(0);
        record.setVersion(0);
        record.setRetryCount(0);
        record.store();
        return record.getId();
    }

    private static Connection newConnection() {
        try {
            return DriverManager.getConnection(
                    SharedContainers.pgJdbcUrl(),
                    SharedContainers.pgUsername(),
                    SharedContainers.pgPassword());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
