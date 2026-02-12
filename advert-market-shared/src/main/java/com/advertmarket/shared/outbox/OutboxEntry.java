package com.advertmarket.shared.outbox;

import com.advertmarket.shared.FenumGroup;
import com.advertmarket.shared.model.DealId;
import java.time.Instant;
import lombok.Builder;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Transactional outbox entry for reliable Kafka publishing.
 *
 * @param id database-generated primary key (null before persist)
 * @param dealId associated deal identifier (nullable)
 * @param idempotencyKey deduplication key for Kafka publishing (nullable)
 * @param topic target Kafka topic
 * @param partitionKey Kafka partition key, usually deal_id (nullable)
 * @param payload serialized event payload as JSON string
 * @param status current processing status
 * @param retryCount number of retry attempts so far
 * @param version optimistic locking version
 * @param createdAt timestamp when the entry was created
 * @param processedAt timestamp when the entry was delivered (nullable)
 */
@Builder
public record OutboxEntry(
        @Nullable Long id,
        @Nullable DealId dealId,
        @Nullable String idempotencyKey,
        @Fenum(FenumGroup.TOPIC_NAME) @NonNull String topic,
        @Nullable String partitionKey,
        @NonNull String payload,
        @NonNull OutboxStatus status,
        int retryCount,
        int version,
        @NonNull Instant createdAt,
        @Nullable Instant processedAt) {
}
