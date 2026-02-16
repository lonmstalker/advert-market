package com.advertmarket.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.event.EventTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KafkaEventTypeConfig")
class KafkaEventTypeConfigTest {

    private final KafkaEventTypeConfig config =
            new KafkaEventTypeConfig();

    @Test
    @DisplayName("Registry has 21 event types registered")
    void registrySize() {
        var registry = config.eventTypeRegistry();
        assertThat(registry.size()).isEqualTo(21);
    }

    @Test
    @DisplayName("PAYOUT_COMPLETED is registered")
    void payoutCompletedRegistered() {
        var registry = config.eventTypeRegistry();
        assertThat(registry.resolve(
                EventTypes.PAYOUT_COMPLETED)).isNotNull();
    }

    @Test
    @DisplayName("REFUND_COMPLETED is registered")
    void refundCompletedRegistered() {
        var registry = config.eventTypeRegistry();
        assertThat(registry.resolve(
                EventTypes.REFUND_COMPLETED)).isNotNull();
    }

    @Test
    @DisplayName("PUBLICATION_RESULT is registered")
    void publicationResultRegistered() {
        var registry = config.eventTypeRegistry();
        assertThat(registry.resolve(
                EventTypes.PUBLICATION_RESULT)).isNotNull();
    }

    @Test
    @DisplayName("RECONCILIATION_RESULT is registered")
    void reconciliationResultRegistered() {
        var registry = config.eventTypeRegistry();
        assertThat(registry.resolve(
                EventTypes.RECONCILIATION_RESULT)).isNotNull();
    }
}
