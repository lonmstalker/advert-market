package com.advertmarket.financial.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.Leg;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.BalanceCachePort;
import com.advertmarket.financial.ledger.repository.JooqAccountBalanceRepository;
import com.advertmarket.financial.ledger.repository.JooqLedgerRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.util.IdempotencyKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("LedgerService")
@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private JooqLedgerRepository ledgerRepository;
    @Mock
    private JooqAccountBalanceRepository balanceRepository;
    @Mock
    private BalanceCachePort balanceCache;
    @Mock
    private MetricsFacade metricsFacade;

    @InjectMocks
    private LedgerService ledgerService;

    private final DealId dealId = DealId.generate();
    private final AccountId externalTon = AccountId.externalTon();
    private final AccountId escrow = AccountId.escrow(dealId);

    @Nested
    @DisplayName("Transfer")
    class Transfer {

        @Test
        @DisplayName("Should record a balanced two-leg transfer")
        void balancedTwoLeg() {
            when(ledgerRepository.tryInsertIdempotencyKey(any()))
                    .thenReturn(true);
            when(balanceRepository.upsertBalanceUnchecked(any(), anyLong()))
                    .thenReturn(0L);

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("tx1"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(1000), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(1000), Leg.Side.CREDIT)),
                    null);

            UUID txRef = ledgerService.transfer(request);

            assertThat(txRef).isNotNull();
            verify(ledgerRepository).insertEntries(
                    eq(txRef), any(), eq(dealId), any(), any());
            verify(metricsFacade).incrementCounter(
                    MetricNames.LEDGER_ENTRY_CREATED);
        }

        @Test
        @DisplayName("Should return existing txRef on duplicate idempotency key")
        void duplicateIdempotency() {
            UUID existingRef = UUID.randomUUID();
            when(ledgerRepository.tryInsertIdempotencyKey(any()))
                    .thenReturn(false);
            when(ledgerRepository.findTxRefByIdempotencyKey(any()))
                    .thenReturn(Optional.of(existingRef));

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("dup"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(1000), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(1000), Leg.Side.CREDIT)),
                    null);

            UUID result = ledgerService.transfer(request);

            assertThat(result).isEqualTo(existingRef);
            verify(ledgerRepository, never()).insertEntries(
                    any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw LEDGER_INCONSISTENCY when debit != credit")
        void unbalancedTransfer() {
            when(ledgerRepository.tryInsertIdempotencyKey(any()))
                    .thenReturn(true);

            var legs = List.of(
                    new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                            Money.ofNano(1000), Leg.Side.DEBIT),
                    new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                            Money.ofNano(999), Leg.Side.CREDIT));

            TransferRequest request = new TransferRequest(
                    dealId, IdempotencyKey.deposit("unbal"), legs, null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("LEDGER_INCONSISTENCY"));
        }

        @Test
        @DisplayName("Should throw INSUFFICIENT_BALANCE when source lacks funds")
        void insufficientBalance() {
            when(ledgerRepository.tryInsertIdempotencyKey(any()))
                    .thenReturn(true);
            when(balanceRepository.upsertBalanceNonNegative(
                    eq(escrow), anyLong()))
                    .thenReturn(OptionalLong.empty());

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(1000), Leg.Side.DEBIT),
                            new Leg(AccountId.ownerPending(new UserId(1L)),
                                    EntryType.OWNER_PAYOUT,
                                    Money.ofNano(1000), Leg.Side.CREDIT)),
                    null);

            assertThatThrownBy(() -> ledgerService.transfer(request))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INSUFFICIENT_BALANCE"));
        }

        @Test
        @DisplayName("Should allow negative balance for EXTERNAL_TON")
        void negativeBalanceContraAccount() {
            when(ledgerRepository.tryInsertIdempotencyKey(any()))
                    .thenReturn(true);
            when(balanceRepository.upsertBalanceUnchecked(
                    eq(externalTon), eq(-1000L)))
                    .thenReturn(-1000L);
            when(balanceRepository.upsertBalanceUnchecked(
                    eq(escrow), eq(1000L)))
                    .thenReturn(1000L);

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("neg"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(1000), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(1000), Leg.Side.CREDIT)),
                    null);

            UUID txRef = ledgerService.transfer(request);

            assertThat(txRef).isNotNull();
            verify(balanceRepository).upsertBalanceUnchecked(
                    externalTon, -1000L);
            verify(balanceRepository, never())
                    .upsertBalanceNonNegative(eq(externalTon), anyLong());
        }

        @Test
        @DisplayName("Should support multi-leg transfer with 3+ legs")
        void multiLegTransfer() {
            when(ledgerRepository.tryInsertIdempotencyKey(any()))
                    .thenReturn(true);
            when(balanceRepository.upsertBalanceNonNegative(
                    eq(escrow), eq(1000L)))
                    .thenReturn(OptionalLong.of(0L));
            when(balanceRepository.upsertBalanceUnchecked(any(), anyLong()))
                    .thenReturn(0L);

            AccountId ownerPending = AccountId.ownerPending(new UserId(1L));
            AccountId treasury = AccountId.platformTreasury();

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.release(dealId),
                    List.of(
                            new Leg(escrow, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(1000), Leg.Side.DEBIT),
                            new Leg(ownerPending, EntryType.OWNER_PAYOUT,
                                    Money.ofNano(900), Leg.Side.CREDIT),
                            new Leg(treasury, EntryType.PLATFORM_COMMISSION,
                                    Money.ofNano(100), Leg.Side.CREDIT)),
                    null);

            UUID txRef = ledgerService.transfer(request);

            assertThat(txRef).isNotNull();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Leg>> legsCaptor =
                    ArgumentCaptor.forClass(List.class);
            verify(ledgerRepository).insertEntries(
                    eq(txRef), any(), any(), any(), legsCaptor.capture());
            assertThat(legsCaptor.getValue()).hasSize(3);
        }

        @Test
        @DisplayName("Should sort ALL legs by accountId for deadlock prevention")
        void allLegsSorted() {
            when(ledgerRepository.tryInsertIdempotencyKey(any()))
                    .thenReturn(true);
            AccountId accountA = AccountId.escrow(DealId.generate());
            AccountId accountB = AccountId.escrow(DealId.generate());
            AccountId creditAccount = AccountId.externalTon();

            String first = accountA.value().compareTo(accountB.value()) < 0
                    ? accountA.value() : accountB.value();
            String second = first.equals(accountA.value())
                    ? accountB.value() : accountA.value();

            AccountId sortedFirst = new AccountId(first);
            AccountId sortedSecond = new AccountId(second);

            // ESCROW:xxx < EXTERNAL_TON alphabetically
            assertThat(sortedSecond.value())
                    .isLessThan(creditAccount.value());

            when(balanceRepository.upsertBalanceNonNegative(
                    eq(sortedFirst), eq(500L)))
                    .thenReturn(OptionalLong.of(500L));
            when(balanceRepository.upsertBalanceNonNegative(
                    eq(sortedSecond), eq(500L)))
                    .thenReturn(OptionalLong.of(500L));
            when(balanceRepository.upsertBalanceUnchecked(any(), anyLong()))
                    .thenReturn(0L);

            TransferRequest request = TransferRequest.balanced(
                    null,
                    IdempotencyKey.deposit("sort-test"),
                    List.of(
                            new Leg(creditAccount,
                                    EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(1000), Leg.Side.CREDIT),
                            new Leg(sortedSecond, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(500), Leg.Side.DEBIT),
                            new Leg(sortedFirst, EntryType.ESCROW_RELEASE,
                                    Money.ofNano(500), Leg.Side.DEBIT)),
                    null);

            ledgerService.transfer(request);

            var order = org.mockito.Mockito.inOrder(balanceRepository);
            order.verify(balanceRepository).upsertBalanceNonNegative(
                    sortedFirst, 500L);
            order.verify(balanceRepository).upsertBalanceNonNegative(
                    sortedSecond, 500L);
            order.verify(balanceRepository).upsertBalanceUnchecked(
                    creditAccount, 1000L);
        }

        @Test
        @DisplayName("Should increment LEDGER_ENTRY_CREATED metric")
        void metricIncremented() {
            when(ledgerRepository.tryInsertIdempotencyKey(any()))
                    .thenReturn(true);
            when(balanceRepository.upsertBalanceUnchecked(any(), anyLong()))
                    .thenReturn(0L);

            TransferRequest request = TransferRequest.balanced(
                    dealId,
                    IdempotencyKey.deposit("metric"),
                    List.of(
                            new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(1000), Leg.Side.DEBIT),
                            new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                    Money.ofNano(1000), Leg.Side.CREDIT)),
                    null);

            ledgerService.transfer(request);

            verify(metricsFacade).incrementCounter(
                    MetricNames.LEDGER_ENTRY_CREATED);
        }
    }

    @Nested
    @DisplayName("Balance queries")
    class BalanceQueries {

        @Test
        @DisplayName("Should read balance from cache when available")
        void cacheHit() {
            when(balanceCache.get(escrow))
                    .thenReturn(OptionalLong.of(5000L));

            long balance = ledgerService.getBalance(escrow);

            assertThat(balance).isEqualTo(5000L);
            verify(balanceRepository, never()).getBalance(any());
        }

        @Test
        @DisplayName("Should fallback to DB on cache miss")
        void cacheMiss() {
            when(balanceCache.get(escrow))
                    .thenReturn(OptionalLong.empty());
            when(balanceRepository.getBalance(escrow))
                    .thenReturn(3000L);

            long balance = ledgerService.getBalance(escrow);

            assertThat(balance).isEqualTo(3000L);
            verify(balanceCache).put(escrow, 3000L);
        }
    }

    @Nested
    @DisplayName("Entry queries")
    class EntryQueries {

        @Test
        @DisplayName("Should return entries by deal")
        void entriesByDeal() {
            var entries = List.of(
                    new LedgerEntry(1L, dealId, escrow,
                            EntryType.ESCROW_DEPOSIT, 0, 1000,
                            "key", UUID.randomUUID(), null,
                            Instant.now()));
            when(ledgerRepository.findByDealId(dealId))
                    .thenReturn(entries);

            var result = ledgerService.getEntriesByDeal(dealId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return paginated entries by account")
        void paginatedEntries() {
            when(ledgerRepository.findByAccountId(escrow, null, 50))
                    .thenReturn(CursorPage.empty());

            var result = ledgerService.getEntriesByAccount(
                    escrow, null, 0);

            assertThat(result.items()).isEmpty();
            assertThat(result.hasMore()).isFalse();
        }

        @Test
        @DisplayName("Should throw INVALID_CURSOR on non-numeric cursor")
        void shouldThrowOnInvalidCursor() {
            assertThatThrownBy(() -> ledgerService.getEntriesByAccount(
                    escrow, "not-a-number", 10))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(
                            ((DomainException) ex).getErrorCode())
                            .isEqualTo("INVALID_CURSOR"));
        }

        @Test
        @DisplayName("Should cap limit at MAX_PAGE_SIZE (200)")
        void shouldCapLimitAtMaxPageSize() {
            when(ledgerRepository.findByAccountId(escrow, null, 200))
                    .thenReturn(CursorPage.empty());

            ledgerService.getEntriesByAccount(escrow, null, 1_000_000);

            verify(ledgerRepository).findByAccountId(escrow, null, 200);
        }

        @Test
        @DisplayName("Should use DEFAULT_PAGE_SIZE when limit is 0")
        void shouldUseDefaultPageSizeWhenZero() {
            when(ledgerRepository.findByAccountId(escrow, null, 50))
                    .thenReturn(CursorPage.empty());

            ledgerService.getEntriesByAccount(escrow, null, 0);

            verify(ledgerRepository).findByAccountId(escrow, null, 50);
        }
    }
}
