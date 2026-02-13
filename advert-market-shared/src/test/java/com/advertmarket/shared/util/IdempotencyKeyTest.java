package com.advertmarket.shared.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.UserId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IdempotencyKey for ledger operations")
class IdempotencyKeyTest {

    private static final DealId DEAL_ID = DealId.of(
            UUID.fromString(
                    "550e8400-e29b-41d4-a716-446655440000"));
    private static final UserId USER_ID = new UserId(42L);
    private static final String TX_HASH = "abc123def456";

    @Test
    @DisplayName("deposit key format")
    void deposit_keyFormat() {
        assertThat(IdempotencyKey.deposit(TX_HASH).value())
                .isEqualTo("deposit:" + TX_HASH);
    }

    @Test
    @DisplayName("partialDeposit key format")
    void partialDeposit_keyFormat() {
        var key = IdempotencyKey.partialDeposit(
                DEAL_ID, TX_HASH);
        assertThat(key.value()).isEqualTo(
                "partial-deposit:" + DEAL_ID.value()
                        + ":" + TX_HASH);
    }

    @Test
    @DisplayName("promote key format")
    void promote_keyFormat() {
        assertThat(
                IdempotencyKey.promote(DEAL_ID).value())
                .isEqualTo("promote:" + DEAL_ID.value());
    }

    @Test
    @DisplayName("release key format")
    void release_keyFormat() {
        assertThat(
                IdempotencyKey.release(DEAL_ID).value())
                .isEqualTo("release:" + DEAL_ID.value());
    }

    @Test
    @DisplayName("refund key format")
    void refund_keyFormat() {
        assertThat(
                IdempotencyKey.refund(DEAL_ID).value())
                .isEqualTo("refund:" + DEAL_ID.value());
    }

    @Test
    @DisplayName("partialRefund key format")
    void partialRefund_keyFormat() {
        assertThat(
                IdempotencyKey.partialRefund(DEAL_ID).value())
                .isEqualTo(
                        "partial-refund:" + DEAL_ID.value());
    }

    @Test
    @DisplayName("overpaymentRefund key format")
    void overpaymentRefund_keyFormat() {
        var key = IdempotencyKey.overpaymentRefund(
                DEAL_ID, TX_HASH);
        assertThat(key.value()).isEqualTo(
                "overpayment-refund:" + DEAL_ID.value()
                        + ":" + TX_HASH);
    }

    @Test
    @DisplayName("lateDepositRefund key format")
    void lateDepositRefund_keyFormat() {
        var key = IdempotencyKey.lateDepositRefund(
                DEAL_ID, TX_HASH);
        assertThat(key.value()).isEqualTo(
                "late-deposit-refund:" + DEAL_ID.value()
                        + ":" + TX_HASH);
    }

    @Test
    @DisplayName("sweep key format")
    void sweep_keyFormat() {
        var accountId = AccountId.commission(DEAL_ID);
        var key = IdempotencyKey.sweep(
                "2026-01-15", accountId);
        assertThat(key.value()).isEqualTo(
                "sweep:2026-01-15:" + accountId.value());
    }

    @Test
    @DisplayName("withdrawal key format")
    void withdrawal_keyFormat() {
        var key = IdempotencyKey.withdrawal(
                USER_ID, "1706486400");
        assertThat(key.value()).isEqualTo(
                "withdrawal:42:1706486400");
    }

    @Test
    @DisplayName("fee key format")
    void fee_keyFormat() {
        assertThat(IdempotencyKey.fee(TX_HASH).value())
                .isEqualTo("fee:" + TX_HASH);
    }

    @Test
    @DisplayName("reversal key format")
    void reversal_keyFormat() {
        assertThat(
                IdempotencyKey.reversal("tx-ref-123").value())
                .isEqualTo("reversal:tx-ref-123");
    }

    @Test
    @DisplayName("Blank value is rejected")
    void blankValue_isRejected() {
        assertThatThrownBy(() -> new IdempotencyKey("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Blank txHash in deposit is rejected")
    void blankTxHash_inDeposit_isRejected() {
        assertThatThrownBy(
                () -> IdempotencyKey.deposit("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Blank txHash in fee is rejected")
    void blankTxHash_inFee_isRejected() {
        assertThatThrownBy(
                () -> IdempotencyKey.fee(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Blank date in sweep is rejected")
    void blankDate_inSweep_isRejected() {
        assertThatThrownBy(
                () -> IdempotencyKey.sweep(
                        " ", AccountId.commission(DEAL_ID)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Blank originalTxRef in reversal is rejected")
    void blankOriginalTxRef_inReversal_isRejected() {
        assertThatThrownBy(
                () -> IdempotencyKey.reversal(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("payout key format")
    void payout_keyFormat() {
        assertThat(
                IdempotencyKey.payout(DEAL_ID).value())
                .isEqualTo("payout:" + DEAL_ID.value());
    }

    @Test
    @DisplayName("publish key format")
    void publish_keyFormat() {
        assertThat(
                IdempotencyKey.publish(DEAL_ID).value())
                .isEqualTo("publish:" + DEAL_ID.value());
    }

    @Test
    @DisplayName("reconciliation key format")
    void reconciliation_keyFormat() {
        var triggerId = UUID.fromString(
                "660e8400-e29b-41d4-a716-446655440001");
        assertThat(
                IdempotencyKey.reconciliation(triggerId).value())
                .isEqualTo("recon:" + triggerId);
    }

    @Test
    @DisplayName("toString returns the value")
    void toString_returnsValue() {
        assertThat(IdempotencyKey.deposit("hash").toString())
                .isEqualTo("deposit:hash");
    }
}
