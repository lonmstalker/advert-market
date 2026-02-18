package com.advertmarket.deal.repository;

import static com.advertmarket.db.generated.tables.Deals.DEALS;
import static com.advertmarket.db.generated.tables.Users.USERS;

import com.advertmarket.deal.api.dto.DealListCriteria;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.deal.api.port.DealRepository;
import com.advertmarket.deal.mapper.DealRecordMapper;
import com.advertmarket.deal.mapper.DealRow;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import com.advertmarket.shared.pagination.CursorCodec;
import java.time.Duration;
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
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

/**
 * Implements {@link DealRepository} using jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class JooqDealRepository implements DealRepository {

    private final DSLContext dsl;
    private final DealRecordMapper dealRecordMapper;

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
                                ? JSONB.valueOf(record.creativeBrief())
                                : null)
                .execute();
    }

    @Override
    @NonNull
    public Optional<DealRecord> findById(@NonNull DealId dealId) {
        return dsl.select(
                        DEALS.ID.as("id"),
                        DEALS.CHANNEL_ID.as("channelId"),
                        DEALS.ADVERTISER_ID.as("advertiserId"),
                        DEALS.OWNER_ID.as("ownerId"),
                        DEALS.PRICING_RULE_ID.as("pricingRuleId"),
                        DEALS.STATUS.as("status"),
                        DEALS.AMOUNT_NANO.as("amountNano"),
                        DEALS.COMMISSION_RATE_BP.as("commissionRateBp"),
                        DEALS.COMMISSION_NANO.as("commissionNano"),
                        DEALS.DEPOSIT_ADDRESS.as("depositAddress"),
                        DEALS.SUBWALLET_ID.as("subwalletId"),
                        DEALS.CREATIVE_BRIEF.as("creativeBrief"),
                        DEALS.CREATIVE_DRAFT.as("creativeDraft"),
                        DEALS.MESSAGE_ID.as("messageId"),
                        DEALS.CONTENT_HASH.as("contentHash"),
                        DEALS.DEADLINE_AT.as("deadlineAt"),
                        DEALS.PUBLISHED_AT.as("publishedAt"),
                        DEALS.COMPLETED_AT.as("completedAt"),
                        DEALS.FUNDED_AT.as("fundedAt"),
                        DEALS.CANCELLATION_REASON.as("cancellationReason"),
                        DEALS.DEPOSIT_TX_HASH.as("depositTxHash"),
                        DEALS.PAYOUT_TX_HASH.as("payoutTxHash"),
                        DEALS.REFUNDED_TX_HASH.as("refundedTxHash"),
                        DEALS.VERSION.as("version"),
                        DEALS.CREATED_AT.as("createdAt"),
                        DEALS.UPDATED_AT.as("updatedAt"))
                .from(DEALS)
                .where(DEALS.ID.eq(dealId.value()))
                .fetchOptionalInto(DealRow.class)
                .map(dealRecordMapper::toRecord);
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
    public void setCancellationReason(@NonNull DealId dealId,
                                       @NonNull String reason) {
        dsl.update(DEALS)
                .set(DEALS.CANCELLATION_REASON, reason)
                .set(DEALS.UPDATED_AT, OffsetDateTime.now())
                .where(DEALS.ID.eq(dealId.value()))
                .execute();
    }

    @Override
    @NonNull
    public List<DealRecord> listByUser(long userId,
                                        @NonNull DealListCriteria criteria) {
        var query = dsl.select(
                        DEALS.ID.as("id"),
                        DEALS.CHANNEL_ID.as("channelId"),
                        DEALS.ADVERTISER_ID.as("advertiserId"),
                        DEALS.OWNER_ID.as("ownerId"),
                        DEALS.PRICING_RULE_ID.as("pricingRuleId"),
                        DEALS.STATUS.as("status"),
                        DEALS.AMOUNT_NANO.as("amountNano"),
                        DEALS.COMMISSION_RATE_BP.as("commissionRateBp"),
                        DEALS.COMMISSION_NANO.as("commissionNano"),
                        DEALS.DEPOSIT_ADDRESS.as("depositAddress"),
                        DEALS.SUBWALLET_ID.as("subwalletId"),
                        DEALS.CREATIVE_BRIEF.as("creativeBrief"),
                        DEALS.CREATIVE_DRAFT.as("creativeDraft"),
                        DEALS.MESSAGE_ID.as("messageId"),
                        DEALS.CONTENT_HASH.as("contentHash"),
                        DEALS.DEADLINE_AT.as("deadlineAt"),
                        DEALS.PUBLISHED_AT.as("publishedAt"),
                        DEALS.COMPLETED_AT.as("completedAt"),
                        DEALS.FUNDED_AT.as("fundedAt"),
                        DEALS.CANCELLATION_REASON.as("cancellationReason"),
                        DEALS.DEPOSIT_TX_HASH.as("depositTxHash"),
                        DEALS.PAYOUT_TX_HASH.as("payoutTxHash"),
                        DEALS.REFUNDED_TX_HASH.as("refundedTxHash"),
                        DEALS.VERSION.as("version"),
                        DEALS.CREATED_AT.as("createdAt"),
                        DEALS.UPDATED_AT.as("updatedAt"))
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

        var rows = query
                .orderBy(DEALS.CREATED_AT.desc(), DEALS.ID.desc())
                .limit(criteria.limit())
                .fetchInto(DealRow.class);
        return rows.stream()
                .map(dealRecordMapper::toRecord)
                .toList();
    }

    @Override
    public boolean reassignOwnerIfNonTerminal(
            @NonNull DealId dealId,
            long newOwnerId) {
        int updated = dsl.update(DEALS)
                .set(DEALS.OWNER_ID, newOwnerId)
                .set(DEALS.VERSION, DEALS.VERSION.plus(1))
                .set(DEALS.UPDATED_AT, OffsetDateTime.now())
                .where(DEALS.ID.eq(dealId.value()))
                .and(DEALS.OWNER_ID.ne(newOwnerId))
                .and(DEALS.STATUS.notIn(
                        DealStatus.COMPLETED_RELEASED.name(),
                        DealStatus.CANCELLED.name(),
                        DealStatus.REFUNDED.name(),
                        DealStatus.PARTIALLY_REFUNDED.name(),
                        DealStatus.EXPIRED.name()))
                .execute();
        return updated > 0;
    }

    @Override
    @NonNull
    public List<DealRecord> findExpiredDeals(
            int batchSize,
            @NonNull Duration gracePeriod) {
        var terminalStatuses = List.of(
                DealStatus.COMPLETED_RELEASED.name(),
                DealStatus.CANCELLED.name(),
                DealStatus.REFUNDED.name(),
                DealStatus.PARTIALLY_REFUNDED.name(),
                DealStatus.EXPIRED.name());
        var deadlineThreshold = OffsetDateTime.now().minus(gracePeriod);

        return dsl.select(
                        DEALS.ID.as("id"),
                        DEALS.CHANNEL_ID.as("channelId"),
                        DEALS.ADVERTISER_ID.as("advertiserId"),
                        DEALS.OWNER_ID.as("ownerId"),
                        DEALS.PRICING_RULE_ID.as("pricingRuleId"),
                        DEALS.STATUS.as("status"),
                        DEALS.AMOUNT_NANO.as("amountNano"),
                        DEALS.COMMISSION_RATE_BP.as("commissionRateBp"),
                        DEALS.COMMISSION_NANO.as("commissionNano"),
                        DEALS.DEPOSIT_ADDRESS.as("depositAddress"),
                        DEALS.SUBWALLET_ID.as("subwalletId"),
                        DEALS.CREATIVE_BRIEF.as("creativeBrief"),
                        DEALS.CREATIVE_DRAFT.as("creativeDraft"),
                        DEALS.MESSAGE_ID.as("messageId"),
                        DEALS.CONTENT_HASH.as("contentHash"),
                        DEALS.DEADLINE_AT.as("deadlineAt"),
                        DEALS.PUBLISHED_AT.as("publishedAt"),
                        DEALS.COMPLETED_AT.as("completedAt"),
                        DEALS.FUNDED_AT.as("fundedAt"),
                        DEALS.CANCELLATION_REASON.as("cancellationReason"),
                        DEALS.DEPOSIT_TX_HASH.as("depositTxHash"),
                        DEALS.PAYOUT_TX_HASH.as("payoutTxHash"),
                        DEALS.REFUNDED_TX_HASH.as("refundedTxHash"),
                        DEALS.VERSION.as("version"),
                        DEALS.CREATED_AT.as("createdAt"),
                        DEALS.UPDATED_AT.as("updatedAt"))
                .from(DEALS)
                .where(DEALS.DEADLINE_AT.le(deadlineThreshold))
                .and(DEALS.STATUS.notIn(terminalStatuses))
                .orderBy(DEALS.DEADLINE_AT.asc())
                .limit(batchSize)
                .forUpdate().skipLocked()
                .fetchInto(DealRow.class)
                .stream()
                .map(dealRecordMapper::toRecord)
                .toList();
    }

    @Override
    public void setDeadline(@NonNull DealId dealId, @NonNull Instant deadlineAt) {
        dsl.update(DEALS)
                .set(DEALS.DEADLINE_AT, OffsetDateTime.ofInstant(deadlineAt, ZoneOffset.UTC))
                .set(DEALS.UPDATED_AT, OffsetDateTime.now())
                .where(DEALS.ID.eq(dealId.value()))
                .execute();
    }

    @Override
    public void clearDeadline(@NonNull DealId dealId) {
        dsl.update(DEALS)
                .setNull(DEALS.DEADLINE_AT)
                .set(DEALS.UPDATED_AT, OffsetDateTime.now())
                .where(DEALS.ID.eq(dealId.value()))
                .execute();
    }

    @Override
    public void setDepositAddress(
            @NonNull DealId dealId,
            @NonNull String depositAddress,
            int subwalletId) {
        dsl.update(DEALS)
                .set(DEALS.DEPOSIT_ADDRESS, depositAddress)
                .set(DEALS.SUBWALLET_ID, subwalletId)
                .set(DEALS.UPDATED_AT, OffsetDateTime.now())
                .where(DEALS.ID.eq(dealId.value()))
                .execute();
    }

    @Override
    public void setFunded(
            @NonNull DealId dealId,
            @NonNull Instant fundedAt,
            @NonNull String depositTxHash) {
        dsl.update(DEALS)
                .set(DEALS.FUNDED_AT, OffsetDateTime.ofInstant(fundedAt, ZoneOffset.UTC))
                .set(DEALS.DEPOSIT_TX_HASH, depositTxHash)
                .set(DEALS.UPDATED_AT, OffsetDateTime.now())
                .where(DEALS.ID.eq(dealId.value()))
                .execute();
    }

    @Override
    public void setPayoutTxHash(
            @NonNull DealId dealId,
            @NonNull String payoutTxHash) {
        dsl.update(DEALS)
                .set(DEALS.PAYOUT_TX_HASH, payoutTxHash)
                .set(DEALS.UPDATED_AT, OffsetDateTime.now())
                .where(DEALS.ID.eq(dealId.value()))
                .execute();
    }

    @Override
    public void setRefundedTxHash(
            @NonNull DealId dealId,
            @NonNull String refundedTxHash) {
        dsl.update(DEALS)
                .set(DEALS.REFUNDED_TX_HASH, refundedTxHash)
                .set(DEALS.UPDATED_AT, OffsetDateTime.now())
                .where(DEALS.ID.eq(dealId.value()))
                .execute();
    }

    @Override
    public void setPublicationMetadata(
            @NonNull DealId dealId,
            long messageId,
            @NonNull String contentHash,
            @NonNull Instant publishedAt) {
        dsl.update(DEALS)
                .set(DEALS.MESSAGE_ID, messageId)
                .set(DEALS.CONTENT_HASH, contentHash)
                .set(DEALS.PUBLISHED_AT, OffsetDateTime.ofInstant(publishedAt, ZoneOffset.UTC))
                .set(DEALS.UPDATED_AT, OffsetDateTime.now())
                .where(DEALS.ID.eq(dealId.value()))
                .execute();
    }

    @Override
    @NonNull
    public List<Long> findOperatorUserIds() {
        return dsl.select(USERS.ID)
                .from(USERS)
                .where(USERS.IS_OPERATOR.isTrue())
                .orderBy(USERS.ID.asc())
                .fetch(USERS.ID);
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
}
