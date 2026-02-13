package com.advertmarket.integration.support;

import static com.advertmarket.db.generated.tables.ChannelMemberships.CHANNEL_MEMBERSHIPS;
import static com.advertmarket.db.generated.tables.ChannelPricingRules.CHANNEL_PRICING_RULES;
import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static com.advertmarket.db.generated.tables.PricingRulePostTypes.PRICING_RULE_POST_TYPES;
import static com.advertmarket.db.generated.tables.Users.USERS;

import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.shared.model.UserId;
import org.jooq.DSLContext;

/**
 * Factory for common test data insertion.
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    /**
     * Inserts a minimal user (first_name = "Test", language_code = "en").
     * Uses {@code ON CONFLICT DO NOTHING} for idempotency.
     */
    public static void upsertUser(DSLContext dsl, long userId) {
        upsertUser(dsl, userId, "Test", null);
    }

    /**
     * Inserts a user with specified fields.
     * Uses {@code ON CONFLICT DO NOTHING} for idempotency.
     */
    public static void upsertUser(DSLContext dsl, long userId,
                                   String firstName, String username) {
        var step = dsl.insertInto(USERS)
                .set(USERS.ID, userId)
                .set(USERS.FIRST_NAME, firstName)
                .set(USERS.LANGUAGE_CODE, "en");
        if (username != null) {
            step = step.set(USERS.USERNAME, username);
        }
        step.onConflictDoNothing().execute();
    }

    /**
     * Inserts a channel with an OWNER membership.
     */
    public static void insertChannelWithOwner(DSLContext dsl,
                                               long channelId,
                                               long ownerId) {
        dsl.insertInto(CHANNELS)
                .set(CHANNELS.ID, channelId)
                .set(CHANNELS.TITLE, "Test Channel")
                .set(CHANNELS.SUBSCRIBER_COUNT, 5000)
                .set(CHANNELS.OWNER_ID, ownerId)
                .execute();
        dsl.insertInto(CHANNEL_MEMBERSHIPS)
                .set(CHANNEL_MEMBERSHIPS.CHANNEL_ID, channelId)
                .set(CHANNEL_MEMBERSHIPS.USER_ID, ownerId)
                .set(CHANNEL_MEMBERSHIPS.ROLE, "OWNER")
                .execute();
    }

    /**
     * Inserts a pricing rule with a single post type.
     *
     * @return the generated rule ID
     */
    public static long insertPricingRule(DSLContext dsl,
                                          long channelId,
                                          String name,
                                          String postType,
                                          long priceNano,
                                          int sortOrder) {
        long ruleId = dsl.insertInto(CHANNEL_PRICING_RULES)
                .set(CHANNEL_PRICING_RULES.CHANNEL_ID, channelId)
                .set(CHANNEL_PRICING_RULES.NAME, name)
                .set(CHANNEL_PRICING_RULES.PRICE_NANO, priceNano)
                .set(CHANNEL_PRICING_RULES.SORT_ORDER, sortOrder)
                .returning(CHANNEL_PRICING_RULES.ID)
                .fetchSingle()
                .getId();
        dsl.insertInto(PRICING_RULE_POST_TYPES)
                .set(PRICING_RULE_POST_TYPES.PRICING_RULE_ID, ruleId)
                .set(PRICING_RULE_POST_TYPES.POST_TYPE, postType)
                .execute();
        return ruleId;
    }

    /**
     * Generates a JWT token for the specified user.
     */
    public static String jwt(JwtTokenProvider jwtTokenProvider,
                              long userId) {
        return jwtTokenProvider.generateToken(
                new UserId(userId), false);
    }
}
