package com.advertmarket.integration.financial;

import static com.advertmarket.db.generated.tables.LedgerEntries.LEDGER_ENTRIES;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.ledger.repository.JooqLedgerRepository;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link JooqLedgerRepository}.
 * PG-only — no Spring context required.
 */
@DisplayName("JooqLedgerRepository — PostgreSQL integration")
class JooqLedgerRepositoryIntegrationTest {

    private static final long ONE_TON = 1_000_000_000L;

    private static DSLContext dsl;
    private static JooqLedgerRepository repository;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
        repository = new JooqLedgerRepository(dsl);
    }

    @BeforeEach
    void cleanUp() {
        DatabaseSupport.cleanFinancialTables(dsl);
    }

    @Nested
    @DisplayName("Idempotency key operations")
    class IdempotencyKeyOperations {

        @Test
        @DisplayName("Should return true on first insert of idempotency key")
        void firstInsert() {
            boolean result = repository.tryInsertIdempotencyKey(
                    "deposit:tx-001");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false on duplicate idempotency key")
        void duplicateInsert() {
            repository.tryInsertIdempotencyKey("deposit:tx-dup");

            boolean result = repository.tryInsertIdempotencyKey(
                    "deposit:tx-dup");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should find txRef after insertEntries")
        void findTxRefAfterInsert() {
            String key = "deposit:tx-find";
            repository.tryInsertIdempotencyKey(key);

            UUID txRef = UUID.randomUUID();
            DealId dealId = DealId.generate();
            repository.insertEntries(
                    txRef, key, dealId, null,
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.escrow(dealId),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)));

            Optional<UUID> found = repository.findTxRefByIdempotencyKey(key);

            assertThat(found).hasValue(txRef);
        }

        @Test
        @DisplayName("Should return empty txRef for orphan idempotency key")
        void orphanKey() {
            repository.tryInsertIdempotencyKey("deposit:orphan");

            Optional<UUID> found = repository.findTxRefByIdempotencyKey(
                    "deposit:orphan");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Entry insertion")
    class EntryInsertion {

        @Test
        @DisplayName("Should insert multi-leg entries under one txRef")
        void multiLeg() {
            String key = "release:multi-leg";
            repository.tryInsertIdempotencyKey(key);
            DealId dealId = DealId.generate();
            UUID txRef = UUID.randomUUID();
            AccountId escrow = AccountId.escrow(dealId);
            AccountId ownerPending = AccountId.ownerPending(
                    new UserId(1L));
            AccountId treasury = AccountId.platformTreasury();

            repository.insertEntries(txRef, key, dealId, "Release", List.of(
                    new Leg(escrow, EntryType.ESCROW_RELEASE,
                            Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                    new Leg(ownerPending, EntryType.OWNER_PAYOUT,
                            Money.ofNano(900_000_000L), Leg.Side.CREDIT),
                    new Leg(treasury, EntryType.PLATFORM_COMMISSION,
                            Money.ofNano(100_000_000L), Leg.Side.CREDIT)));

            int count = dsl.selectCount()
                    .from(LEDGER_ENTRIES)
                    .where(LEDGER_ENTRIES.TX_REF.eq(txRef))
                    .fetchOne(0, int.class);
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should save NULL dealId correctly")
        void nullDealId() {
            String key = "fee:null-deal";
            repository.tryInsertIdempotencyKey(key);
            UUID txRef = UUID.randomUUID();

            repository.insertEntries(txRef, key, null, null, List.of(
                    new Leg(AccountId.networkFees(),
                            EntryType.NETWORK_FEE,
                            Money.ofNano(50_000L), Leg.Side.DEBIT),
                    new Leg(AccountId.externalTon(),
                            EntryType.NETWORK_FEE,
                            Money.ofNano(50_000L), Leg.Side.CREDIT)));

            UUID dealUuid = dsl.select(LEDGER_ENTRIES.DEAL_ID)
                    .from(LEDGER_ENTRIES)
                    .where(LEDGER_ENTRIES.TX_REF.eq(txRef))
                    .limit(1)
                    .fetchOne(LEDGER_ENTRIES.DEAL_ID);
            assertThat(dealUuid).isNull();
        }

        @Test
        @DisplayName("Should persist non-null description")
        void nonNullDescription() {
            String key = "deposit:desc";
            repository.tryInsertIdempotencyKey(key);
            DealId dealId = DealId.generate();
            UUID txRef = UUID.randomUUID();

            repository.insertEntries(txRef, key, dealId,
                    "Test deposit description",
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.escrow(dealId),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)));

            String desc = dsl.select(LEDGER_ENTRIES.DESCRIPTION)
                    .from(LEDGER_ENTRIES)
                    .where(LEDGER_ENTRIES.TX_REF.eq(txRef))
                    .limit(1)
                    .fetchOne(LEDGER_ENTRIES.DESCRIPTION);
            assertThat(desc).isEqualTo("Test deposit description");
        }

        @Test
        @DisplayName("Should persist null description")
        void nullDescription() {
            String key = "deposit:null-desc";
            repository.tryInsertIdempotencyKey(key);
            DealId dealId = DealId.generate();
            UUID txRef = UUID.randomUUID();

            repository.insertEntries(txRef, key, dealId, null,
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.escrow(dealId),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)));

            String desc = dsl.select(LEDGER_ENTRIES.DESCRIPTION)
                    .from(LEDGER_ENTRIES)
                    .where(LEDGER_ENTRIES.TX_REF.eq(txRef))
                    .limit(1)
                    .fetchOne(LEDGER_ENTRIES.DESCRIPTION);
            assertThat(desc).isNull();
        }
    }

    @Nested
    @DisplayName("findByDealId")
    class FindByDealId {

        @Test
        @DisplayName("Should return entries ordered by created_at DESC, id DESC")
        void ordering() {
            DealId dealId = DealId.generate();
            for (int i = 0; i < 3; i++) {
                String key = "deposit:order-" + i;
                repository.tryInsertIdempotencyKey(key);
                repository.insertEntries(UUID.randomUUID(), key, dealId, null,
                        List.of(
                                new Leg(AccountId.externalTon(),
                                        EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.DEBIT),
                                new Leg(AccountId.escrow(dealId),
                                        EntryType.ESCROW_DEPOSIT,
                                        Money.ofNano(ONE_TON),
                                        Leg.Side.CREDIT)));
            }

            List<LedgerEntry> entries = repository.findByDealId(dealId);

            assertThat(entries).hasSize(6);
            for (int i = 0; i < entries.size() - 1; i++) {
                assertThat(entries.get(i).id())
                        .isGreaterThan(entries.get(i + 1).id());
            }
        }

        @Test
        @DisplayName("Should not return entries from another deal")
        void isolation() {
            DealId deal1 = DealId.generate();
            DealId deal2 = DealId.generate();
            insertDeposit(deal1, "deposit:iso-1");
            insertDeposit(deal2, "deposit:iso-2");

            List<LedgerEntry> entries = repository.findByDealId(deal1);

            assertThat(entries).hasSize(2);
            assertThat(entries).allSatisfy(
                    e -> assertThat(e.dealId()).isEqualTo(deal1));
        }

        @Test
        @DisplayName("Should return empty list for non-existent deal")
        void nonExistentDeal() {
            List<LedgerEntry> entries = repository.findByDealId(
                    DealId.generate());

            assertThat(entries).isEmpty();
        }
    }

    @Nested
    @DisplayName("Cursor pagination (findByAccountId)")
    class CursorPagination {

        @Test
        @DisplayName("Should return hasMore=false when entries <= limit")
        void noMorePages() {
            AccountId account = AccountId.externalTon();
            insertDeposit(DealId.generate(), "deposit:pg-1");
            insertDeposit(DealId.generate(), "deposit:pg-2");

            CursorPage<LedgerEntry> page = repository.findByAccountId(
                    account, null, 10);

            assertThat(page.items()).hasSize(2);
            assertThat(page.hasMore()).isFalse();
            assertThat(page.nextCursor()).isNull();
        }

        @Test
        @DisplayName("Should return hasMore=true and nextCursor when entries > limit")
        void morePages() {
            AccountId account = AccountId.externalTon();
            for (int i = 0; i < 5; i++) {
                insertDeposit(DealId.generate(), "deposit:mp-" + i);
            }

            CursorPage<LedgerEntry> page = repository.findByAccountId(
                    account, null, 3);

            assertThat(page.items()).hasSize(3);
            assertThat(page.hasMore()).isTrue();
            assertThat(page.nextCursor()).isNotNull();
        }

        @Test
        @DisplayName("Should filter by cursor ID")
        void cursorFilters() {
            AccountId account = AccountId.externalTon();
            for (int i = 0; i < 5; i++) {
                insertDeposit(DealId.generate(), "deposit:cf-" + i);
            }

            CursorPage<LedgerEntry> page1 = repository.findByAccountId(
                    account, null, 3);
            CursorPage<LedgerEntry> page2 = repository.findByAccountId(
                    account, page1.nextCursor(), 10);

            long lastIdPage1 = page1.items().getLast().id();
            assertThat(page2.items()).allSatisfy(
                    e -> assertThat(e.id()).isLessThan(lastIdPage1));
        }

        @Test
        @DisplayName("Should return empty page when cursor past all entries")
        void cursorPastAll() {
            AccountId account = AccountId.externalTon();
            insertDeposit(DealId.generate(), "deposit:past-1");

            CursorPage<LedgerEntry> page = repository.findByAccountId(
                    account, "1", 10);

            assertThat(page.items()).isEmpty();
            assertThat(page.hasMore()).isFalse();
        }

        @Test
        @DisplayName("Should isolate entries by account_id")
        void accountIsolation() {
            DealId deal1 = DealId.generate();
            DealId deal2 = DealId.generate();
            insertDeposit(deal1, "deposit:ai-1");
            insertDeposit(deal2, "deposit:ai-2");

            AccountId escrow1 = AccountId.escrow(deal1);
            CursorPage<LedgerEntry> page = repository.findByAccountId(
                    escrow1, null, 10);

            assertThat(page.items()).hasSize(1);
            assertThat(page.items().getFirst().accountId())
                    .isEqualTo(escrow1);
        }
    }

    @Nested
    @DisplayName("Entry mapping")
    class EntryMapping {

        @Test
        @DisplayName("Should map all LedgerEntry fields correctly")
        void fullMapping() {
            DealId dealId = DealId.generate();
            String key = "deposit:map-all";
            repository.tryInsertIdempotencyKey(key);
            UUID txRef = UUID.randomUUID();

            repository.insertEntries(txRef, key, dealId,
                    "Mapping test",
                    List.of(
                            new Leg(AccountId.externalTon(),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                            new Leg(AccountId.escrow(dealId),
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(ONE_TON), Leg.Side.CREDIT)));

            List<LedgerEntry> entries = repository.findByDealId(dealId);
            LedgerEntry debitEntry = entries.stream()
                    .filter(e -> e.debitNano() > 0)
                    .findFirst().orElseThrow();

            assertThat(debitEntry.id()).isPositive();
            assertThat(debitEntry.dealId()).isEqualTo(dealId);
            assertThat(debitEntry.accountId())
                    .isEqualTo(AccountId.externalTon());
            assertThat(debitEntry.entryType())
                    .isEqualTo(EntryType.ESCROW_DEPOSIT);
            assertThat(debitEntry.debitNano()).isEqualTo(ONE_TON);
            assertThat(debitEntry.creditNano()).isZero();
            assertThat(debitEntry.idempotencyKey()).isEqualTo(key);
            assertThat(debitEntry.txRef()).isEqualTo(txRef);
            assertThat(debitEntry.description())
                    .isEqualTo("Mapping test");
            assertThat(debitEntry.createdAt()).isNotNull();
        }

        @Test
        @DisplayName("Should map null dealId to null in LedgerEntry")
        void nullDealIdMapping() {
            String key = "fee:null-map";
            repository.tryInsertIdempotencyKey(key);
            UUID txRef = UUID.randomUUID();

            repository.insertEntries(txRef, key, null, null,
                    List.of(
                            new Leg(AccountId.networkFees(),
                                    EntryType.NETWORK_FEE,
                                    Money.ofNano(50_000L), Leg.Side.DEBIT),
                            new Leg(AccountId.externalTon(),
                                    EntryType.NETWORK_FEE,
                                    Money.ofNano(50_000L), Leg.Side.CREDIT)));

            CursorPage<LedgerEntry> page = repository.findByAccountId(
                    AccountId.networkFees(), null, 10);
            assertThat(page.items()).hasSize(1);
            assertThat(page.items().getFirst().dealId()).isNull();
        }

        @Test
        @DisplayName("Should support all 17 EntryType enum values through valueOf")
        void allEntryTypes() {
            assertThat(EntryType.values()).hasSize(17);
            for (EntryType type : EntryType.values()) {
                assertThat(EntryType.valueOf(type.name()))
                        .isEqualTo(type);
            }
        }
    }

    private void insertDeposit(DealId dealId, String idempotencyKey) {
        repository.tryInsertIdempotencyKey(idempotencyKey);
        repository.insertEntries(
                UUID.randomUUID(), idempotencyKey, dealId, null,
                List.of(
                        new Leg(AccountId.externalTon(),
                                EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(ONE_TON), Leg.Side.DEBIT),
                        new Leg(AccountId.escrow(dealId),
                                EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(ONE_TON), Leg.Side.CREDIT)));
    }
}
