package com.advertmarket.deal.adapter;

import static com.advertmarket.db.generated.tables.Deals.DEALS;

import com.advertmarket.deal.api.port.DealAuthorizationPort;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * ABAC authorization adapter for deal access checks.
 */
@Component
@RequiredArgsConstructor
public class DealAuthorizationAdapter implements DealAuthorizationPort {

    private final DSLContext dsl;

    @Override
    public boolean isParticipant(@NonNull DealId dealId) {
        long userId = SecurityContextUtil.currentUserId().value();
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(DEALS)
                        .where(DEALS.ID.eq(dealId.value()))
                        .and(DEALS.ADVERTISER_ID.eq(userId)
                                .or(DEALS.OWNER_ID.eq(userId))));
    }

    @Override
    public boolean isAdvertiser(@NonNull DealId dealId) {
        long userId = SecurityContextUtil.currentUserId().value();
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(DEALS)
                        .where(DEALS.ID.eq(dealId.value()))
                        .and(DEALS.ADVERTISER_ID.eq(userId)));
    }

    @Override
    public boolean isOwner(@NonNull DealId dealId) {
        long userId = SecurityContextUtil.currentUserId().value();
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(DEALS)
                        .where(DEALS.ID.eq(dealId.value()))
                        .and(DEALS.OWNER_ID.eq(userId)));
    }

    @Override
    public long getChannelId(@NonNull DealId dealId) {
        var result = dsl.select(DEALS.CHANNEL_ID)
                .from(DEALS)
                .where(DEALS.ID.eq(dealId.value()))
                .fetchOne(DEALS.CHANNEL_ID);
        if (result == null) {
            throw new IllegalStateException(
                    "Deal not found: " + dealId.value());
        }
        return result;
    }
}
