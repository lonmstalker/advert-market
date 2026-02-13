package com.advertmarket.financial.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PayoutCompletedEvent")
class PayoutCompletedEventTest {

    @Test
    @DisplayName("Implements DomainEvent")
    void implementsDomainEvent() {
        var event = new PayoutCompletedEvent(
                "tx123", 1_000_000_000L, 50_000_000L,
                "EQAddress", 3);
        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    @DisplayName("All fields accessible")
    void allFieldsAccessible() {
        var event = new PayoutCompletedEvent(
                "tx456", 2_000_000_000L, 100_000_000L,
                "EQAddr2", 5);

        assertThat(event.txHash()).isEqualTo("tx456");
        assertThat(event.amountNano()).isEqualTo(2_000_000_000L);
        assertThat(event.commissionNano()).isEqualTo(100_000_000L);
        assertThat(event.toAddress()).isEqualTo("EQAddr2");
        assertThat(event.confirmations()).isEqualTo(5);
    }
}
