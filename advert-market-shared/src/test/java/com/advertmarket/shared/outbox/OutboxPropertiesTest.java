package com.advertmarket.shared.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OutboxProperties — defaults and validation")
class OutboxPropertiesTest {

    @Test
    @DisplayName("Applies default values when nulls provided")
    void defaultValues() {
        var props = new OutboxProperties(null, 0, 0, null, null, 0);

        assertThat(props.pollInterval())
                .isEqualTo(Duration.ofMillis(500));
        assertThat(props.batchSize()).isEqualTo(50);
        assertThat(props.maxRetries()).isEqualTo(3);
        assertThat(props.initialBackoff())
                .isEqualTo(Duration.ofSeconds(1));
        assertThat(props.publishTimeout())
                .isEqualTo(Duration.ofSeconds(5));
        assertThat(props.stuckThresholdSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("Preserves explicit values")
    void explicitValues() {
        var props = new OutboxProperties(
                Duration.ofSeconds(2), 100, 5,
                Duration.ofSeconds(3), Duration.ofSeconds(10), 600);

        assertThat(props.pollInterval())
                .isEqualTo(Duration.ofSeconds(2));
        assertThat(props.batchSize()).isEqualTo(100);
        assertThat(props.maxRetries()).isEqualTo(5);
        assertThat(props.initialBackoff())
                .isEqualTo(Duration.ofSeconds(3));
        assertThat(props.publishTimeout())
                .isEqualTo(Duration.ofSeconds(10));
        assertThat(props.stuckThresholdSeconds()).isEqualTo(600);
    }

    @Test
    @DisplayName("Negative batchSize falls back to default")
    void negativeBatchSize_fallsBackToDefault() {
        var props = new OutboxProperties(
                Duration.ofSeconds(1), -1, 1,
                Duration.ofMillis(500), null, 0);

        assertThat(props.batchSize()).isEqualTo(50);
    }
}
