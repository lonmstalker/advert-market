package com.advertmarket.deal.repository;

import static com.advertmarket.db.generated.tables.DealEvents.DEAL_EVENTS;

import com.advertmarket.deal.api.dto.DealEventRecord;
import com.advertmarket.deal.api.port.DealEventRepository;
import com.advertmarket.shared.model.DealId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * Implements {@link DealEventRepository} using jOOQ (append-only).
 */
@Repository
@RequiredArgsConstructor
public class JooqDealEventRepository implements DealEventRepository {

    private final DSLContext dsl;

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
        return dsl.select(
                        DEAL_EVENTS.ID, DEAL_EVENTS.DEAL_ID,
                        DEAL_EVENTS.EVENT_TYPE, DEAL_EVENTS.FROM_STATUS,
                        DEAL_EVENTS.TO_STATUS, DEAL_EVENTS.ACTOR_ID,
                        DEAL_EVENTS.ACTOR_TYPE, DEAL_EVENTS.PAYLOAD,
                        DEAL_EVENTS.CREATED_AT)
                .from(DEAL_EVENTS)
                .where(DEAL_EVENTS.DEAL_ID.eq(dealId.value()))
                .orderBy(DEAL_EVENTS.CREATED_AT.desc())
                .fetch(this::toEventRecord);
    }

    private DealEventRecord toEventRecord(Record r) {
        return new DealEventRecord(
                r.get(DEAL_EVENTS.ID),
                r.get(DEAL_EVENTS.DEAL_ID),
                r.get(DEAL_EVENTS.EVENT_TYPE),
                r.get(DEAL_EVENTS.FROM_STATUS),
                r.get(DEAL_EVENTS.TO_STATUS),
                r.get(DEAL_EVENTS.ACTOR_ID),
                r.get(DEAL_EVENTS.ACTOR_TYPE),
                r.get(DEAL_EVENTS.PAYLOAD) != null
                        ? r.get(DEAL_EVENTS.PAYLOAD).data()
                        : "{}",
                r.get(DEAL_EVENTS.CREATED_AT).toInstant());
    }
}
