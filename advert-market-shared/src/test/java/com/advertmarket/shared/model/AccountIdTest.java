package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountId value object")
class AccountIdTest {

    private static final DealId DEAL_ID = DealId.of(
            UUID.fromString(
                    "550e8400-e29b-41d4-a716-446655440000"));
    private static final UserId USER_ID = new UserId(123456789L);

    @Test
    @DisplayName("platformTreasury returns cached singleton")
    void platformTreasury_returnsCachedSingleton() {
        AccountId a = AccountId.platformTreasury();
        AccountId b = AccountId.platformTreasury();
        assertThat(a).isSameAs(b);
        assertThat(a.value()).isEqualTo("PLATFORM_TREASURY");
        assertThat(a.type())
                .isEqualTo(AccountType.PLATFORM_TREASURY);
    }

    @Test
    @DisplayName("externalTon returns cached singleton")
    void externalTon_returnsCachedSingleton() {
        AccountId a = AccountId.externalTon();
        AccountId b = AccountId.externalTon();
        assertThat(a).isSameAs(b);
        assertThat(a.value()).isEqualTo("EXTERNAL_TON");
        assertThat(a.type())
                .isEqualTo(AccountType.EXTERNAL_TON);
    }

    @Test
    @DisplayName("networkFees returns cached singleton")
    void networkFees_returnsCachedSingleton() {
        AccountId a = AccountId.networkFees();
        AccountId b = AccountId.networkFees();
        assertThat(a).isSameAs(b);
        assertThat(a.value()).isEqualTo("NETWORK_FEES");
        assertThat(a.type())
                .isEqualTo(AccountType.NETWORK_FEES);
    }

    @Test
    @DisplayName("dustWriteoff returns cached singleton")
    void dustWriteoff_returnsCachedSingleton() {
        AccountId a = AccountId.dustWriteoff();
        AccountId b = AccountId.dustWriteoff();
        assertThat(a).isSameAs(b);
        assertThat(a.value()).isEqualTo("DUST_WRITEOFF");
        assertThat(a.type())
                .isEqualTo(AccountType.DUST_WRITEOFF);
    }

    @Test
    @DisplayName("escrow creates deal-scoped account")
    void escrow_createsDealScopedAccount() {
        AccountId id = AccountId.escrow(DEAL_ID);
        assertThat(id.value()).isEqualTo(
                "ESCROW:" + DEAL_ID.value());
        assertThat(id.type()).isEqualTo(AccountType.ESCROW);
    }

    @Test
    @DisplayName("ownerPending creates user-scoped account")
    void ownerPending_createsUserScopedAccount() {
        AccountId id = AccountId.ownerPending(USER_ID);
        assertThat(id.value()).isEqualTo(
                "OWNER_PENDING:" + USER_ID.value());
        assertThat(id.type())
                .isEqualTo(AccountType.OWNER_PENDING);
    }

    @Test
    @DisplayName("commission creates deal-scoped account")
    void commission_createsDealScopedAccount() {
        AccountId id = AccountId.commission(DEAL_ID);
        assertThat(id.value()).isEqualTo(
                "COMMISSION:" + DEAL_ID.value());
        assertThat(id.type())
                .isEqualTo(AccountType.COMMISSION);
    }

    @Test
    @DisplayName("overpayment creates deal-scoped account")
    void overpayment_createsDealScopedAccount() {
        AccountId id = AccountId.overpayment(DEAL_ID);
        assertThat(id.value()).isEqualTo(
                "OVERPAYMENT:" + DEAL_ID.value());
        assertThat(id.type())
                .isEqualTo(AccountType.OVERPAYMENT);
    }

    @Test
    @DisplayName("partialDeposit creates deal-scoped account")
    void partialDeposit_createsDealScopedAccount() {
        AccountId id = AccountId.partialDeposit(DEAL_ID);
        assertThat(id.value()).isEqualTo(
                "PARTIAL_DEPOSIT:" + DEAL_ID.value());
        assertThat(id.type())
                .isEqualTo(AccountType.PARTIAL_DEPOSIT);
    }

    @Test
    @DisplayName("lateDeposit creates deal-scoped account")
    void lateDeposit_createsDealScopedAccount() {
        AccountId id = AccountId.lateDeposit(DEAL_ID);
        assertThat(id.value()).isEqualTo(
                "LATE_DEPOSIT:" + DEAL_ID.value());
        assertThat(id.type())
                .isEqualTo(AccountType.LATE_DEPOSIT);
    }

    @Test
    @DisplayName("Blank value is rejected")
    void blankValue_isRejected() {
        assertThatThrownBy(() -> new AccountId("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Null value is rejected")
    void nullValue_isRejected() {
        assertThatThrownBy(() -> new AccountId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("toString returns the value string")
    void toString_returnsValue() {
        assertThat(AccountId.platformTreasury().toString())
                .isEqualTo("PLATFORM_TREASURY");
    }

    @Test
    @DisplayName("Unknown prefix throws IllegalStateException")
    void unknownPrefix_throwsIllegalState() {
        var id = new AccountId("UNKNOWN_TYPE");
        assertThatThrownBy(id::type)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UNKNOWN_TYPE");
    }

    @Test
    @DisplayName("Singleton prefix with suffix resolves type")
    void singletonPrefixWithSuffix_resolvesType() {
        var id = new AccountId("EXTERNAL_TON:unexpected");
        assertThat(id.type())
                .isEqualTo(AccountType.EXTERNAL_TON);
    }
}
