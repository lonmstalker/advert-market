package com.advertmarket.financial.ton.repository;

import static com.advertmarket.db.generated.tables.Deals.DEALS;
import static com.advertmarket.db.generated.tables.PiiStore.PII_STORE;
import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.selectOne;

import com.advertmarket.financial.ton.service.UnclaimedPayoutCandidate;
import com.advertmarket.shared.model.DealStatus;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

/**
 * Read-model queries for unclaimed payout scheduler.
 */
@Repository
@RequiredArgsConstructor
public class UnclaimedPayoutRepository {

    private static final String TON_ADDRESS_FIELD = "ton_address";

    private final DSLContext dsl;

    /**
     * Finds completed deals with missing payout tx hash.
     */
    public @NonNull List<UnclaimedPayoutCandidate> findOpenPayoutCandidates(int limit) {
        Field<Boolean> hasTonAddress = exists(selectOne()
                .from(PII_STORE)
                .where(PII_STORE.USER_ID.eq(DEALS.OWNER_ID))
                .and(PII_STORE.FIELD_NAME.eq(TON_ADDRESS_FIELD)))
                .as("has_ton_address");

        return dsl.select(
                        DEALS.ID,
                        DEALS.OWNER_ID,
                        DEALS.AMOUNT_NANO,
                        DEALS.COMMISSION_NANO,
                        DEALS.SUBWALLET_ID,
                        DEALS.COMPLETED_AT,
                        hasTonAddress)
                .from(DEALS)
                .where(DEALS.STATUS.eq(DealStatus.COMPLETED_RELEASED.name()))
                .and(DEALS.PAYOUT_TX_HASH.isNull())
                .and(DEALS.REFUNDED_TX_HASH.isNull())
                .and(DEALS.COMPLETED_AT.isNotNull())
                .orderBy(DEALS.COMPLETED_AT.asc())
                .limit(limit)
                .fetch(record -> new UnclaimedPayoutCandidate(
                        Objects.requireNonNull(record.get(DEALS.ID)),
                        Objects.requireNonNull(record.get(DEALS.OWNER_ID)),
                        Objects.requireNonNull(record.get(DEALS.AMOUNT_NANO)),
                        Objects.requireNonNull(record.get(DEALS.COMMISSION_NANO)),
                        Objects.requireNonNullElse(record.get(DEALS.SUBWALLET_ID), 0),
                        Objects.requireNonNull(record.get(DEALS.COMPLETED_AT))
                                .toInstant(),
                        Objects.requireNonNullElse(record.get(hasTonAddress), false)));
    }

    /**
     * Returns active operators for day-30 escalation notifications.
     */
    public @NonNull List<Long> findOperatorUserIds() {
        return dsl.select(USERS.ID)
                .from(USERS)
                .where(USERS.IS_OPERATOR.isTrue())
                .fetch(USERS.ID)
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
