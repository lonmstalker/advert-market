package com.advertmarket.shared.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.model.DealId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OutboxEntry — record construction and builder")
class OutboxEntryTest {

    @Test
    @DisplayName("Builder creates entry with all fields")
    void builder_allFields() {
        var dealId = DealId.generate();
        var now = Instant.now();

        var entry = OutboxEntry.builder()
                .id(1L)
                .dealId(dealId)
                .idempotencyKey("idem-123")
                .topic(TopicNames.DEAL_STATE_CHANGED)
                .partitionKey(dealId.toString())
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(1)
                .createdAt(now)
                .processedAt(null)
                .build();

        assertThat(entry.id()).isEqualTo(1L);
        assertThat(entry.dealId()).isEqualTo(dealId);
        assertThat(entry.topic())
                .isEqualTo(TopicNames.DEAL_STATE_CHANGED);
        assertThat(entry.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entry.createdAt()).isEqualTo(now);
        assertThat(entry.processedAt()).isNull();
    }

    @Test
    @DisplayName("Builder creates entry with nullable fields as null")
    void builder_nullableFields() {
        var entry = OutboxEntry.builder()
                .topic(TopicNames.FINANCIAL_COMMANDS)
                .payload("{\"test\":true}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(0)
                .createdAt(Instant.now())
                .build();

        assertThat(entry.id()).isNull();
        assertThat(entry.dealId()).isNull();
        assertThat(entry.idempotencyKey()).isNull();
        assertThat(entry.partitionKey()).isNull();
    }
}
