package com.advertmarket.financial.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RefundCompletedEvent")
class RefundCompletedEventTest {

    @Test
    @DisplayName("Implements DomainEvent")
    void implementsDomainEvent() {
        var event = new RefundCompletedEvent(
                "tx123", 1_000_000_000L, "EQAddress", 3);
        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    @DisplayName("All fields accessible")
    void allFieldsAccessible() {
        var event = new RefundCompletedEvent(
                "tx789", 500_000_000L, "EQAddr3", 7);

        assertThat(event.txHash()).isEqualTo("tx789");
        assertThat(event.amountNano()).isEqualTo(500_000_000L);
        assertThat(event.toAddress()).isEqualTo("EQAddr3");
        assertThat(event.confirmations()).isEqualTo(7);
    }
}
