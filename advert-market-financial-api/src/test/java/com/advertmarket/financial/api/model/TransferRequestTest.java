package com.advertmarket.financial.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.Money;
import com.advertmarket.shared.util.IdempotencyKey;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TransferRequest")
class TransferRequestTest {

    private final DealId dealId = DealId.generate();
    private final IdempotencyKey key = IdempotencyKey.deposit("test");
    private final AccountId externalTon = AccountId.externalTon();
    private final AccountId escrow = AccountId.escrow(dealId);

    @Test
    @DisplayName("Should create balanced transfer request")
    void balancedTransfer() {
        var request = TransferRequest.balanced(dealId, key,
                List.of(
                        new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(1000), Leg.Side.DEBIT),
                        new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(1000), Leg.Side.CREDIT)),
                "desc");

        assertThat(request.legs()).hasSize(2);
        assertThat(request.dealId()).isEqualTo(dealId);
        assertThat(request.description()).isEqualTo("desc");
    }

    @Test
    @DisplayName("Should reject unbalanced transfer")
    void rejectUnbalanced() {
        assertThatThrownBy(() -> TransferRequest.balanced(dealId, key,
                List.of(
                        new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(1000), Leg.Side.DEBIT),
                        new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(999), Leg.Side.CREDIT)),
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unbalanced");
    }

    @Test
    @DisplayName("Should reject fewer than 2 legs")
    void rejectSingleLeg() {
        assertThatThrownBy(() -> new TransferRequest(dealId, key,
                List.of(new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                        Money.ofNano(1000), Leg.Side.DEBIT)),
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2");
    }

    @Test
    @DisplayName("Should reject null idempotencyKey")
    void rejectNullKey() {
        assertThatThrownBy(() -> new TransferRequest(dealId, null,
                List.of(
                        new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(1000), Leg.Side.DEBIT),
                        new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(1000), Leg.Side.CREDIT)),
                null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should make defensive copy of legs list")
    void defensiveCopy() {
        var mutableLegs = new ArrayList<>(List.of(
                new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                        Money.ofNano(1000), Leg.Side.DEBIT),
                new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                        Money.ofNano(1000), Leg.Side.CREDIT)));

        var request = new TransferRequest(dealId, key,
                mutableLegs, null);

        mutableLegs.add(new Leg(AccountId.platformTreasury(),
                EntryType.PLATFORM_COMMISSION,
                Money.ofNano(500), Leg.Side.CREDIT));

        assertThat(request.legs()).hasSize(2);
    }

    @Test
    @DisplayName("Should allow null dealId")
    void nullDealId() {
        var request = TransferRequest.balanced(null, key,
                List.of(
                        new Leg(externalTon, EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(1000), Leg.Side.DEBIT),
                        new Leg(escrow, EntryType.ESCROW_DEPOSIT,
                                Money.ofNano(1000), Leg.Side.CREDIT)),
                null);

        assertThat(request.dealId()).isNull();
    }
}
