package com.advertmarket.integration.support;

import static com.advertmarket.db.generated.tables.AccountBalances.ACCOUNT_BALANCES;
import static com.advertmarket.db.generated.tables.ChannelCategories.CHANNEL_CATEGORIES;
import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.DealEvents.DEAL_EVENTS;
import static com.advertmarket.db.generated.tables.Deals.DEALS;
import static com.advertmarket.db.generated.tables.DisputeEvidence.DISPUTE_EVIDENCE;
import static com.advertmarket.db.generated.tables.Disputes.DISPUTES;
import static com.advertmarket.db.generated.tables.LedgerEntries.LEDGER_ENTRIES;
import static com.advertmarket.db.generated.tables.LedgerIdempotencyKeys.LEDGER_IDEMPOTENCY_KEYS;
import static com.advertmarket.db.generated.tables.NotificationOutbox.NOTIFICATION_OUTBOX;
import static com.advertmarket.db.generated.tables.PostingChecks.POSTING_CHECKS;
import static com.advertmarket.db.generated.tables.PricingRulePostTypes.PRICING_RULE_POST_TYPES;
import static com.advertmarket.db.generated.tables.TonTransactions.TON_TRANSACTIONS;
import static com.advertmarket.db.generated.tables.Users.USERS;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Shared database support for integration tests.
 *
 * <p>Uses {@link SharedContainers#POSTGRES} singleton. Performs Liquibase
 * migration once per JVM (thread-safe double-checked locking).
 */
public final class DatabaseSupport {

    private static volatile boolean migrated;

    private static volatile DSLContext sharedDsl;

    private DatabaseSupport() {
    }

    /**
     * Runs Liquibase migration if not yet done. Thread-safe, idempotent.
     */
    public static void ensureMigrated() {
        if (!migrated) {
            synchronized (DatabaseSupport.class) {
                if (!migrated) {
                    runMigration();
                    migrated = true;
                }
            }
        }
    }

    /**
     * Returns a shared DSLContext connected to the singleton Postgres.
     */
    public static DSLContext dsl() {
        if (sharedDsl == null) {
            synchronized (DatabaseSupport.class) {
                if (sharedDsl == null) {
                    sharedDsl = DSL.using(
                            SharedContainers.pgJdbcUrl(),
                            SharedContainers.pgUsername(),
                            SharedContainers.pgPassword());
                }
            }
        }
        return sharedDsl;
    }

    /**
     * Deletes all rows in FK-dependency order (all tables).
     */
    public static void cleanAllTables(DSLContext dsl) {
        cleanDealTables(dsl);
        cleanFinancialTables(dsl);
        dsl.deleteFrom(PRICING_RULE_POST_TYPES).execute();
        dsl.deleteFrom(CHANNEL_PRICING_RULES).execute();
        dsl.deleteFrom(CHANNEL_CATEGORIES).execute();
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS).execute();
        dsl.deleteFrom(CHANNELS).execute();
        dsl.deleteFrom(NOTIFICATION_OUTBOX).execute();
        dsl.deleteFrom(USERS).execute();
    }

    /**
     * Cleans financial tables (ledger entries, balances, idempotency keys).
     * Uses TRUNCATE for ledger_entries since it has an immutability trigger
     * that prevents DELETE.
     */
    public static void cleanFinancialTables(DSLContext dsl) {
        dsl.truncate(LEDGER_ENTRIES).cascade().execute();
        dsl.deleteFrom(ACCOUNT_BALANCES).execute();
        dsl.deleteFrom(LEDGER_IDEMPOTENCY_KEYS).execute();
    }

    /**
     * Deletes marketplace tables (no USERS/NOTIFICATION_OUTBOX).
     */
    public static void cleanMarketplaceTables(DSLContext dsl) {
        dsl.deleteFrom(PRICING_RULE_POST_TYPES).execute();
        dsl.deleteFrom(CHANNEL_PRICING_RULES).execute();
        dsl.deleteFrom(CHANNEL_CATEGORIES).execute();
        dsl.deleteFrom(CHANNEL_MEMBERSHIPS).execute();
        dsl.deleteFrom(CHANNELS).execute();
    }

    /**
     * Cleans deal-related tables in FK-dependency order.
     * Uses TRUNCATE for tables with immutability triggers
     * (deal_events, dispute_evidence, posting_checks).
     */
    public static void cleanDealTables(DSLContext dsl) {
        dsl.truncate(POSTING_CHECKS).cascade().execute();
        dsl.truncate(DISPUTE_EVIDENCE).cascade().execute();
        dsl.deleteFrom(DISPUTES).execute();
        dsl.deleteFrom(TON_TRANSACTIONS).execute();
        dsl.truncate(DEAL_EVENTS).cascade().execute();
        dsl.deleteFrom(DEALS).execute();
    }

    /**
     * Cleans tables required for user repository tests.
     *
     * <p>Historically this removed only {@code users}, but the schema has grown
     * foreign keys referencing users (channels, deals, etc.). Since integration
     * tests share a singleton Postgres, a partial cleanup can violate FK
     * constraints when previous tests inserted related rows.
     */
    public static void cleanUserTables(DSLContext dsl) {
        cleanAllTables(dsl);
    }

    private static void runMigration() {
        try {
            var tempDsl = DSL.using(
                    SharedContainers.pgJdbcUrl(),
                    SharedContainers.pgUsername(),
                    SharedContainers.pgPassword());
            var conn = tempDsl.configuration()
                    .connectionProvider().acquire();
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(
                            new JdbcConnection(conn));
            try (var liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.yaml",
                    new ClassLoaderResourceAccessor(),
                    database)) {
                liquibase.update("");
            }
        } catch (liquibase.exception.LiquibaseException e) {
            throw new IllegalStateException(
                    "Liquibase migration failed", e);
        }
    }
}
