package com.advertmarket.deal.repository;

import static com.advertmarket.db.generated.tables.DealEvents.DEAL_EVENTS;

import com.advertmarket.deal.api.dto.DealEventRecord;
import com.advertmarket.deal.api.port.DealEventRepository;
import com.advertmarket.deal.mapper.DealEventRecordMapper;
import com.advertmarket.deal.mapper.DealEventRow;
import com.advertmarket.shared.model.DealId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.springframework.stereotype.Repository;

/**
 * Implements {@link DealEventRepository} using jOOQ (append-only).
 */
@Repository
@RequiredArgsConstructor
public class JooqDealEventRepository implements DealEventRepository {

    private final DSLContext dsl;
    private final DealEventRecordMapper dealEventRecordMapper;

    @Override
    public void append(@NonNull DealEventRecord record) {
        dsl.insertInto(DEAL_EVENTS)
                .set(DEAL_EVENTS.DEAL_ID, record.dealId())
                .set(DEAL_EVENTS.EVENT_TYPE, record.eventType())
                .set(DEAL_EVENTS.FROM_STATUS, record.fromStatus())
                .set(DEAL_EVENTS.TO_STATUS, record.toStatus())
                .set(DEAL_EVENTS.ACTOR_ID, record.actorId())
                .set(DEAL_EVENTS.ACTOR_TYPE, record.actorType())
                .set(DEAL_EVENTS.PAYLOAD, JSON.valueOf(record.payload()))
                .execute();
    }

    @Override
    @NonNull
    public List<DealEventRecord> findByDealId(@NonNull DealId dealId) {
        var rows = dsl.select(
                        DEAL_EVENTS.ID.as("id"),
                        DEAL_EVENTS.DEAL_ID.as("dealId"),
                        DEAL_EVENTS.EVENT_TYPE.as("eventType"),
                        DEAL_EVENTS.FROM_STATUS.as("fromStatus"),
                        DEAL_EVENTS.TO_STATUS.as("toStatus"),
                        DEAL_EVENTS.ACTOR_ID.as("actorId"),
                        DEAL_EVENTS.ACTOR_TYPE.as("actorType"),
                        DEAL_EVENTS.PAYLOAD.as("payload"),
                        DEAL_EVENTS.CREATED_AT.as("createdAt"))
                .from(DEAL_EVENTS)
                .where(DEAL_EVENTS.DEAL_ID.eq(dealId.value()))
                .orderBy(DEAL_EVENTS.CREATED_AT.desc())
                .fetchInto(DealEventRow.class);
        return rows.stream()
                .map(dealEventRecordMapper::toRecord)
                .toList();
    }
}
