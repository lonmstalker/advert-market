package com.advertmarket.app.config.outbox;

import static com.advertmarket.db.generated.tables.NotificationOutbox.NOTIFICATION_OUTBOX;

import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxRepository;
import com.advertmarket.shared.outbox.OutboxStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * Implementation of {@link OutboxRepository} using jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqOutboxRepository implements OutboxRepository {

    private final DSLContext dsl;

    @Override
    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "dealId null-check is performed before .value() call")
    public void save(@NonNull OutboxEntry entry) {
        dsl.insertInto(NOTIFICATION_OUTBOX)
                .set(NOTIFICATION_OUTBOX.DEAL_ID,
                        entry.dealId() != null
                                ? entry.dealId().value() : null)
                .set(NOTIFICATION_OUTBOX.IDEMPOTENCY_KEY,
                        entry.idempotencyKey())
                .set(NOTIFICATION_OUTBOX.TOPIC, entry.topic())
                .set(NOTIFICATION_OUTBOX.PARTITION_KEY,
                        entry.partitionKey())
                .set(NOTIFICATION_OUTBOX.PAYLOAD,
                        JSON.json(entry.payload()))
                .set(NOTIFICATION_OUTBOX.STATUS,
                        entry.status().name())
                .set(NOTIFICATION_OUTBOX.RETRY_COUNT,
                        entry.retryCount())
                .set(NOTIFICATION_OUTBOX.VERSION,
                        entry.version())
                .execute();
    }

    @Override
    public @NonNull List<OutboxEntry> findPendingBatch(
            int batchSize) {
        return dsl.selectFrom(NOTIFICATION_OUTBOX)
                .where(NOTIFICATION_OUTBOX.STATUS.eq(
                        OutboxStatus.PENDING.name()))
                .and(NOTIFICATION_OUTBOX.CREATED_AT.lessThan(
                        OffsetDateTime.now(ZoneOffset.UTC)
                                .minusSeconds(1)))
                .orderBy(NOTIFICATION_OUTBOX.CREATED_AT.asc())
                .limit(batchSize)
                .forUpdate().skipLocked()
                .fetch(this::toEntry);
    }

    @Override
    public void markDelivered(long id) {
        dsl.update(NOTIFICATION_OUTBOX)
                .set(NOTIFICATION_OUTBOX.STATUS,
                        OutboxStatus.DELIVERED.name())
                .set(NOTIFICATION_OUTBOX.PROCESSED_AT,
                        OffsetDateTime.now(ZoneOffset.UTC))
                .where(NOTIFICATION_OUTBOX.ID.eq(id))
                .execute();
    }

    @Override
    public void markFailed(long id) {
        dsl.update(NOTIFICATION_OUTBOX)
                .set(NOTIFICATION_OUTBOX.STATUS,
                        OutboxStatus.FAILED.name())
                .set(NOTIFICATION_OUTBOX.PROCESSED_AT,
                        OffsetDateTime.now(ZoneOffset.UTC))
                .where(NOTIFICATION_OUTBOX.ID.eq(id))
                .execute();
    }

    @Override
    public void incrementRetry(long id) {
        dsl.update(NOTIFICATION_OUTBOX)
                .set(NOTIFICATION_OUTBOX.RETRY_COUNT,
                        NOTIFICATION_OUTBOX.RETRY_COUNT.plus(1))
                .where(NOTIFICATION_OUTBOX.ID.eq(id))
                .execute();
    }

    private OutboxEntry toEntry(Record record) {
        var dealIdUuid = record.get(NOTIFICATION_OUTBOX.DEAL_ID);
        @Nullable DealId dealId = dealIdUuid != null
                ? DealId.of(dealIdUuid) : null;

        var processedAt = record.get(
                NOTIFICATION_OUTBOX.PROCESSED_AT);
        var createdAt = record.get(NOTIFICATION_OUTBOX.CREATED_AT);

        return OutboxEntry.builder()
                .id(record.get(NOTIFICATION_OUTBOX.ID))
                .dealId(dealId)
                .idempotencyKey(record.get(
                        NOTIFICATION_OUTBOX.IDEMPOTENCY_KEY))
                .topic(record.get(NOTIFICATION_OUTBOX.TOPIC))
                .partitionKey(record.get(
                        NOTIFICATION_OUTBOX.PARTITION_KEY))
                .payload(record.get(NOTIFICATION_OUTBOX.PAYLOAD)
                        .data())
                .status(OutboxStatus.valueOf(
                        record.get(NOTIFICATION_OUTBOX.STATUS)))
                .retryCount(record.get(
                        NOTIFICATION_OUTBOX.RETRY_COUNT))
                .version(record.get(NOTIFICATION_OUTBOX.VERSION))
                .createdAt(toInstant(createdAt))
                .processedAt(processedAt != null
                        ? processedAt.toInstant() : null)
                .build();
    }

    private static Instant toInstant(
            @Nullable OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : Instant.now();
    }
}
