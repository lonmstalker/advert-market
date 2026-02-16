package com.advertmarket.financial.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.TransferRequest;
import com.advertmarket.financial.api.port.LedgerPort;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.util.IdempotencyKey;
import com.advertmarket.shared.pagination.CursorPage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("WalletService — user wallet operations")
class WalletServiceTest {

    private LedgerPort ledgerPort;
    private UserRepository userRepository;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        ledgerPort = mock(LedgerPort.class);
        userRepository = mock(UserRepository.class);
        var metrics = mock(MetricsFacade.class);
        walletService = new WalletService(
                ledgerPort,
                userRepository,
                metrics,
                1_000_000_000L,
                1_000_000_000_000L,
                1_000_000_000_000L);
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("Should return available balance from OWNER_PENDING account")
        void returnsAvailableBalance() {
            var userId = new UserId(42L);
            var ownerPendingAccount = AccountId.ownerPending(userId);

            when(ledgerPort.getBalance(ownerPendingAccount))
                    .thenReturn(5_000_000_000L);

            var summary = walletService.getSummary(userId);

            assertThat(summary.availableBalanceNano()).isEqualTo(5_000_000_000L);
        }

        @Test
        @DisplayName("Should return zero balances for new user with no transactions")
        void returnsZerosForNewUser() {
            var userId = new UserId(99L);
            var ownerPendingAccount = AccountId.ownerPending(userId);

            when(ledgerPort.getBalance(ownerPendingAccount))
                    .thenReturn(0L);

            var summary = walletService.getSummary(userId);

            assertThat(summary.availableBalanceNano()).isZero();
            assertThat(summary.pendingBalanceNano()).isZero();
            assertThat(summary.totalEarnedNano()).isZero();
        }
    }

    @Nested
    @DisplayName("getTransactions")
    class GetTransactions {

        @Test
        @DisplayName("Should return paginated ledger entries for OWNER_PENDING account")
        void returnsPaginatedEntries() {
            var userId = new UserId(42L);
            var ownerPendingAccount = AccountId.ownerPending(userId);
            var entry = new LedgerEntry(
                    1L, null, ownerPendingAccount, EntryType.ESCROW_RELEASE,
                    0L, 1_000_000_000L, "idem-1", UUID.randomUUID(),
                    "Payout", Instant.now());
            var page = new CursorPage<>(List.of(entry), null);

            when(ledgerPort.getEntriesByAccount(ownerPendingAccount, null, 20))
                    .thenReturn(page);

            var result = walletService.getTransactions(userId, null, 20);

            assertThat(result.items()).hasSize(1);
            assertThat(result.hasMore()).isFalse();
        }

        @Test
        @DisplayName("Should forward cursor to LedgerPort")
        void forwardsCursor() {
            var userId = new UserId(42L);
            var ownerPendingAccount = AccountId.ownerPending(userId);
            var page = CursorPage.<LedgerEntry>empty();

            when(ledgerPort.getEntriesByAccount(ownerPendingAccount, "cursor123", 10))
                    .thenReturn(page);

            var result = walletService.getTransactions(userId, "cursor123", 10);

            assertThat(result.items()).isEmpty();
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("Should reject withdrawal above manual approval threshold")
        void shouldRejectAboveManualApprovalThreshold() {
            var userId = new UserId(42L);

            assertThatThrownBy(() ->
                    walletService.withdraw(
                            userId,
                            1_000_000_000_001L,
                            "idem-1"))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.WITHDRAWAL_REQUIRES_MANUAL_REVIEW);
        }

        @Test
        @DisplayName("Should reject withdrawal when daily velocity limit is exceeded")
        void shouldRejectWhenDailyVelocityLimitExceeded() {
            var userId = new UserId(42L);
            var ownerPending = AccountId.ownerPending(userId);

            when(userRepository.findTonAddress(userId))
                    .thenReturn(Optional.of("UQBvW8Z5huBkMJYdnJxrERhVfeLsvKVbcjOx0Z3KPnEr0Xgd"));
            when(ledgerPort.getBalance(ownerPending))
                    .thenReturn(500_000_000_000L);
            when(ledgerPort.sumDebitsSince(
                    eq(ownerPending),
                    eq(EntryType.OWNER_WITHDRAWAL),
                    any(Instant.class)))
                    .thenReturn(999_000_000_000L);

            assertThatThrownBy(() ->
                    walletService.withdraw(
                            userId,
                            2_000_000_000L,
                            "idem-2"))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.WITHDRAWAL_VELOCITY_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("Should create transfer when withdrawal is within limits")
        void shouldCreateTransferWhenWithinLimits() {
            var userId = new UserId(42L);
            var ownerPending = AccountId.ownerPending(userId);

            when(userRepository.findTonAddress(userId))
                    .thenReturn(Optional.of("UQBvW8Z5huBkMJYdnJxrERhVfeLsvKVbcjOx0Z3KPnEr0Xgd"));
            when(ledgerPort.getBalance(ownerPending))
                    .thenReturn(500_000_000_000L);
            when(ledgerPort.sumDebitsSince(
                    eq(ownerPending),
                    eq(EntryType.OWNER_WITHDRAWAL),
                    any(Instant.class)))
                    .thenReturn(100_000_000_000L);
            when(ledgerPort.transfer(org.mockito.ArgumentMatchers.any(TransferRequest.class)))
                    .thenReturn(UUID.randomUUID());

            walletService.withdraw(userId, 2_000_000_000L, "idem-3");

            var captor = ArgumentCaptor.forClass(TransferRequest.class);
            verify(ledgerPort).transfer(captor.capture());
            assertThat(captor.getValue().idempotencyKey().value())
                    .isEqualTo("withdrawal:idem-3");
        }

        @Test
        @DisplayName("Should return existing result for duplicate idempotency key without re-checking balance")
        void shouldReturnExistingForDuplicateIdempotencyKey() {
            var userId = new UserId(42L);
            var existingTxRef = UUID.randomUUID();

            when(userRepository.findTonAddress(userId))
                    .thenReturn(Optional.of("UQBvW8Z5huBkMJYdnJxrERhVfeLsvKVbcjOx0Z3KPnEr0Xgd"));
            when(ledgerPort.findTxRefByIdempotencyKey(
                    new IdempotencyKey("withdrawal:idem-dup")))
                    .thenReturn(Optional.of(existingTxRef));

            var result = walletService.withdraw(userId, 2_000_000_000L, "idem-dup");

            assertThat(result.withdrawalId()).isEqualTo(existingTxRef.toString());
            verify(ledgerPort, never()).getBalance(any(AccountId.class));
            verify(ledgerPort, never()).transfer(any(TransferRequest.class));
        }

        @Test
        @DisplayName("Should reject withdrawal below minimum amount")
        void shouldRejectBelowMinimumAmount() {
            var userId = new UserId(42L);

            assertThatThrownBy(() ->
                    walletService.withdraw(userId, 999_999_999L, "idem-min"))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.WITHDRAWAL_MIN_AMOUNT);
        }

        @Test
        @DisplayName("Should reject withdrawal when balance is insufficient")
        void shouldRejectWhenInsufficientBalance() {
            var userId = new UserId(42L);
            var ownerPending = AccountId.ownerPending(userId);

            when(userRepository.findTonAddress(userId))
                    .thenReturn(Optional.of("UQBvW8Z5huBkMJYdnJxrERhVfeLsvKVbcjOx0Z3KPnEr0Xgd"));
            when(ledgerPort.findTxRefByIdempotencyKey(any()))
                    .thenReturn(Optional.empty());
            when(ledgerPort.getBalance(ownerPending))
                    .thenReturn(1_000_000_000L);

            assertThatThrownBy(() ->
                    walletService.withdraw(userId, 2_000_000_000L, "idem-bal"))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.INSUFFICIENT_BALANCE);
        }

        @Test
        @DisplayName("Should reject withdrawal when user has no TON address")
        void shouldRejectWhenNoTonAddress() {
            var userId = new UserId(42L);

            when(userRepository.findTonAddress(userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    walletService.withdraw(userId, 2_000_000_000L, "idem-addr"))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.WALLET_ADDRESS_REQUIRED);
        }

        @Test
        @DisplayName("Should reject withdrawal when TON address has invalid CRC")
        void shouldRejectWhenTonAddressInvalid() {
            var userId = new UserId(42L);

            when(userRepository.findTonAddress(userId))
                    .thenReturn(Optional.of("UQBvW8Z5huBkMJYdnJxrERhVfeLsvKVbcjOx0Z3KPnEr0dSx"));

            assertThatThrownBy(() ->
                    walletService.withdraw(userId, 2_000_000_000L, "idem-crc"))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCodes.INVALID_TON_ADDRESS);
        }
    }
}
