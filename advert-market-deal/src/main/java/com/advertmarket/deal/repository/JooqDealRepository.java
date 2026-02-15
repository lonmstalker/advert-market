package com.advertmarket.deal.repository;

import static com.advertmarket.db.generated.tables.Deals.DEALS;

import com.advertmarket.deal.api.dto.DealListCriteria;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.pagination.CursorCodec;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.springframework.stereotype.Repository;

/**
 * Implements {@link DealRepository} using jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqDealRepository implements DealRepository {

    private final DSLContext dsl;

    @Override
    public void insert(@NonNull DealRecord record) {
        dsl.insertInto(DEALS)
                .set(DEALS.ID, record.id())
                .set(DEALS.CHANNEL_ID, record.channelId())
                .set(DEALS.ADVERTISER_ID, record.advertiserId())
                .set(DEALS.OWNER_ID, record.ownerId())
                .set(DEALS.PRICING_RULE_ID, record.pricingRuleId())
                .set(DEALS.STATUS, record.status().name())
                .set(DEALS.AMOUNT_NANO, record.amountNano())
                .set(DEALS.COMMISSION_RATE_BP, record.commissionRateBp())
                .set(DEALS.COMMISSION_NANO, record.commissionNano())
                .set(DEALS.CREATIVE_BRIEF,
                        record.creativeBrief() != null
                                ? JSON.valueOf(record.creativeBrief())
                                : null)
                .execute();
    }

    @Override
    @NonNull
    public Optional<DealRecord> findById(@NonNull DealId dealId) {
        return dsl.select(
                        DEALS.ID, DEALS.CHANNEL_ID, DEALS.ADVERTISER_ID,
                        DEALS.OWNER_ID, DEALS.PRICING_RULE_ID, DEALS.STATUS,
                        DEALS.AMOUNT_NANO, DEALS.COMMISSION_RATE_BP,
                        DEALS.COMMISSION_NANO, DEALS.DEPOSIT_ADDRESS,
                        DEALS.SUBWALLET_ID, DEALS.CREATIVE_BRIEF,
                        DEALS.CREATIVE_DRAFT, DEALS.MESSAGE_ID,
                        DEALS.CONTENT_HASH, DEALS.DEADLINE_AT,
                        DEALS.PUBLISHED_AT, DEALS.COMPLETED_AT,
                        DEALS.FUNDED_AT, DEALS.CANCELLATION_REASON,
                        DEALS.DEPOSIT_TX_HASH, DEALS.PAYOUT_TX_HASH,
                        DEALS.REFUNDED_TX_HASH, DEALS.VERSION,
                        DEALS.CREATED_AT, DEALS.UPDATED_AT)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId.value()))
                .fetchOptional(this::toDealRecord);
    }

    @Override
    public int updateStatus(@NonNull DealId dealId,
                            @NonNull DealStatus expectedFrom,
                            @NonNull DealStatus to,
                            int expectedVersion) {
        return dsl.update(DEALS)
                .set(DEALS.STATUS, to.name())
                .set(DEALS.VERSION, DEALS.VERSION.plus(1))
                .set(DEALS.UPDATED_AT, OffsetDateTime.now())
                .where(DEALS.ID.eq(dealId.value()))
                .and(DEALS.STATUS.eq(expectedFrom.name()))
                .and(DEALS.VERSION.eq(expectedVersion))
                .execute();
    }

    @Override
    @NonNull
    public List<DealRecord> listByUser(long userId,
                                        @NonNull DealListCriteria criteria) {
        var query = dsl.select(
                        DEALS.ID, DEALS.CHANNEL_ID, DEALS.ADVERTISER_ID,
                        DEALS.OWNER_ID, DEALS.PRICING_RULE_ID, DEALS.STATUS,
                        DEALS.AMOUNT_NANO, DEALS.COMMISSION_RATE_BP,
                        DEALS.COMMISSION_NANO, DEALS.DEPOSIT_ADDRESS,
                        DEALS.SUBWALLET_ID, DEALS.CREATIVE_BRIEF,
                        DEALS.CREATIVE_DRAFT, DEALS.MESSAGE_ID,
                        DEALS.CONTENT_HASH, DEALS.DEADLINE_AT,
                        DEALS.PUBLISHED_AT, DEALS.COMPLETED_AT,
                        DEALS.FUNDED_AT, DEALS.CANCELLATION_REASON,
                        DEALS.DEPOSIT_TX_HASH, DEALS.PAYOUT_TX_HASH,
                        DEALS.REFUNDED_TX_HASH, DEALS.VERSION,
                        DEALS.CREATED_AT, DEALS.UPDATED_AT)
                .from(DEALS)
                .where(DEALS.ADVERTISER_ID.eq(userId)
                        .or(DEALS.OWNER_ID.eq(userId)));

        var statusFilter = criteria.status();
        if (statusFilter != null) {
            query = query.and(DEALS.STATUS.eq(statusFilter.name()));
        }

        // Keyset pagination by created_at DESC, id DESC (composite cursor)
        var cursor = criteria.cursor();
        if (cursor != null) {
            var fields = decodeCursor(cursor);
            var cursorTs = OffsetDateTime.ofInstant(
                    Instant.parse(fields.get("ts")), ZoneOffset.UTC);
            var cursorId = UUID.fromString(fields.get("id"));
            query = query.and(
                    DEALS.CREATED_AT.lessThan(cursorTs)
                            .or(DEALS.CREATED_AT.eq(cursorTs)
                                    .and(DEALS.ID.lessThan(cursorId))));
        }

        return query
                .orderBy(DEALS.CREATED_AT.desc(), DEALS.ID.desc())
                .limit(criteria.limit())
                .fetch(this::toDealRecord);
    }

    /**
     * Encodes a composite cursor from the last record's created_at and id.
     */
    public static @NonNull String buildCursor(@NonNull DealRecord last) {
        return CursorCodec.encode(Map.of(
                "ts", last.createdAt().toString(),
                "id", last.id().toString()));
    }

    private static Map<String, String> decodeCursor(String cursor) {
        var fields = CursorCodec.decode(cursor);
        if (!fields.containsKey("ts") || !fields.containsKey("id")) {
            throw new DomainException(ErrorCodes.INVALID_CURSOR,
                    "Invalid cursor format");
        }
        return fields;
    }

    private DealRecord toDealRecord(Record r) {
        return new DealRecord(
                r.get(DEALS.ID),
                r.get(DEALS.CHANNEL_ID),
                r.get(DEALS.ADVERTISER_ID),
                r.get(DEALS.OWNER_ID),
                r.get(DEALS.PRICING_RULE_ID),
                DealStatus.valueOf(r.get(DEALS.STATUS)),
                r.get(DEALS.AMOUNT_NANO),
                r.get(DEALS.COMMISSION_RATE_BP),
                r.get(DEALS.COMMISSION_NANO),
                r.get(DEALS.DEPOSIT_ADDRESS),
                r.get(DEALS.SUBWALLET_ID),
                r.get(DEALS.CREATIVE_BRIEF) != null
                        ? r.get(DEALS.CREATIVE_BRIEF).data()
                        : null,
                r.get(DEALS.CREATIVE_DRAFT) != null
                        ? r.get(DEALS.CREATIVE_DRAFT).data()
                        : null,
                r.get(DEALS.MESSAGE_ID),
                r.get(DEALS.CONTENT_HASH),
                r.get(DEALS.DEADLINE_AT) != null
                        ? r.get(DEALS.DEADLINE_AT).toInstant()
                        : null,
                r.get(DEALS.PUBLISHED_AT) != null
                        ? r.get(DEALS.PUBLISHED_AT).toInstant()
                        : null,
                r.get(DEALS.COMPLETED_AT) != null
                        ? r.get(DEALS.COMPLETED_AT).toInstant()
                        : null,
                r.get(DEALS.FUNDED_AT) != null
                        ? r.get(DEALS.FUNDED_AT).toInstant()
                        : null,
                r.get(DEALS.CANCELLATION_REASON),
                r.get(DEALS.DEPOSIT_TX_HASH),
                r.get(DEALS.PAYOUT_TX_HASH),
                r.get(DEALS.REFUNDED_TX_HASH),
                r.get(DEALS.VERSION),
                r.get(DEALS.CREATED_AT).toInstant(),
                r.get(DEALS.UPDATED_AT).toInstant());
    }
}
