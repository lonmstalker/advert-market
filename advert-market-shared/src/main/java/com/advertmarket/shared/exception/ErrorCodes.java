package com.advertmarket.shared.exception;

import com.advertmarket.shared.FenumGroup;
import org.checkerframework.checker.fenum.qual.Fenum;

/**
 * Centralized error code constants for {@link DomainException}.
 */
@SuppressWarnings("fenum:assignment")
public final class ErrorCodes {

    // --- Auth ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String AUTH_INIT_DATA_INVALID =
            "AUTH_INIT_DATA_INVALID";
    public static final @Fenum(FenumGroup.ERROR_CODE) String AUTH_TOKEN_EXPIRED =
            "AUTH_TOKEN_EXPIRED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String AUTH_INVALID_TOKEN =
            "AUTH_INVALID_TOKEN";
    public static final @Fenum(FenumGroup.ERROR_CODE) String AUTH_TOKEN_BLACKLISTED =
            "AUTH_TOKEN_BLACKLISTED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String AUTH_INSUFFICIENT_PERMISSIONS =
            "AUTH_INSUFFICIENT_PERMISSIONS";
    public static final @Fenum(FenumGroup.ERROR_CODE) String USER_BLOCKED =
            "USER_BLOCKED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String ACCOUNT_DELETED =
            "ACCOUNT_DELETED";

    // --- Rate Limiting ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String RATE_LIMIT_EXCEEDED =
            "RATE_LIMIT_EXCEEDED";

    // --- Deal ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String DEAL_NOT_FOUND =
            "DEAL_NOT_FOUND";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DEAL_ALREADY_EXISTS =
            "DEAL_ALREADY_EXISTS";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DEAL_TERMS_MISMATCH =
            "DEAL_TERMS_MISMATCH";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DEAL_DEADLINE_EXPIRED =
            "DEAL_DEADLINE_EXPIRED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DEAL_CANCELLED =
            "DEAL_CANCELLED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DEAL_NOT_PARTICIPANT =
            "DEAL_NOT_PARTICIPANT";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DEAL_ACTOR_NOT_ALLOWED =
            "DEAL_ACTOR_NOT_ALLOWED";

    // --- Financial ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String INSUFFICIENT_BALANCE =
            "INSUFFICIENT_BALANCE";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DEPOSIT_TIMEOUT =
            "DEPOSIT_TIMEOUT";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DEPOSIT_AMOUNT_MISMATCH =
            "DEPOSIT_AMOUNT_MISMATCH";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DEPOSIT_REJECTED =
            "DEPOSIT_REJECTED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String PAYOUT_FAILED =
            "PAYOUT_FAILED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String REFUND_FAILED =
            "REFUND_FAILED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String LEDGER_INCONSISTENCY =
            "LEDGER_INCONSISTENCY";
    public static final @Fenum(FenumGroup.ERROR_CODE) String COMMISSION_CALCULATION_ERROR =
            "COMMISSION_CALCULATION_ERROR";
    public static final @Fenum(FenumGroup.ERROR_CODE) String INVALID_CURSOR =
            "INVALID_CURSOR";

    // --- Channel ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String CHANNEL_NOT_FOUND =
            "CHANNEL_NOT_FOUND";
    public static final @Fenum(FenumGroup.ERROR_CODE) String CHANNEL_ALREADY_REGISTERED =
            "CHANNEL_ALREADY_REGISTERED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String CHANNEL_INACCESSIBLE =
            "CHANNEL_INACCESSIBLE";
    public static final @Fenum(FenumGroup.ERROR_CODE) String CHANNEL_NOT_OWNED =
            "CHANNEL_NOT_OWNED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String CHANNEL_STATS_UNAVAILABLE =
            "CHANNEL_STATS_UNAVAILABLE";
    public static final @Fenum(FenumGroup.ERROR_CODE) String CHANNEL_BOT_NOT_MEMBER =
            "CHANNEL_BOT_NOT_MEMBER";
    public static final @Fenum(FenumGroup.ERROR_CODE) String CHANNEL_BOT_NOT_ADMIN =
            "CHANNEL_BOT_NOT_ADMIN";
    public static final @Fenum(FenumGroup.ERROR_CODE) String CHANNEL_BOT_INSUFFICIENT_RIGHTS =
            "CHANNEL_BOT_INSUFFICIENT_RIGHTS";
    public static final @Fenum(FenumGroup.ERROR_CODE) String CHANNEL_USER_NOT_ADMIN =
            "CHANNEL_USER_NOT_ADMIN";
    public static final @Fenum(FenumGroup.ERROR_CODE) String PRICING_RULE_NOT_FOUND =
            "PRICING_RULE_NOT_FOUND";

    // --- Team ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String TEAM_MEMBER_NOT_FOUND =
            "TEAM_MEMBER_NOT_FOUND";
    public static final @Fenum(FenumGroup.ERROR_CODE) String TEAM_MEMBER_ALREADY_EXISTS =
            "TEAM_MEMBER_ALREADY_EXISTS";
    public static final @Fenum(FenumGroup.ERROR_CODE) String TEAM_LIMIT_EXCEEDED =
            "TEAM_LIMIT_EXCEEDED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String TEAM_OWNER_PROTECTED =
            "TEAM_OWNER_PROTECTED";

    // --- Dispute ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String DISPUTE_NOT_FOUND =
            "DISPUTE_NOT_FOUND";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DISPUTE_ALREADY_EXISTS =
            "DISPUTE_ALREADY_EXISTS";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DISPUTE_RESOLUTION_FAILED =
            "DISPUTE_RESOLUTION_FAILED";

    // --- Creative ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String CREATIVE_NOT_FOUND =
            "CREATIVE_NOT_FOUND";
    public static final @Fenum(FenumGroup.ERROR_CODE) String CREATIVE_INVALID_FORMAT =
            "CREATIVE_INVALID_FORMAT";
    public static final @Fenum(FenumGroup.ERROR_CODE) String CREATIVE_TOO_LARGE =
            "CREATIVE_TOO_LARGE";
    public static final @Fenum(FenumGroup.ERROR_CODE) String CREATIVE_REJECTED =
            "CREATIVE_REJECTED";

    // --- Delivery ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String DELIVERY_VERIFICATION_FAILED =
            "DELIVERY_VERIFICATION_FAILED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DELIVERY_POST_DELETED =
            "DELIVERY_POST_DELETED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DELIVERY_CONTENT_MODIFIED =
            "DELIVERY_CONTENT_MODIFIED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String DELIVERY_TIMEOUT =
            "DELIVERY_TIMEOUT";

    // --- Validation ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String VALIDATION_FAILED =
            "VALIDATION_FAILED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String INVALID_PARAMETER =
            "INVALID_PARAMETER";
    public static final @Fenum(FenumGroup.ERROR_CODE) String MISSING_REQUIRED_FIELD =
            "MISSING_REQUIRED_FIELD";

    // --- Entities ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String ENTITY_NOT_FOUND =
            "ENTITY_NOT_FOUND";
    public static final @Fenum(FenumGroup.ERROR_CODE) String USER_NOT_FOUND =
            "USER_NOT_FOUND";
    public static final @Fenum(FenumGroup.ERROR_CODE) String WALLET_NOT_FOUND =
            "WALLET_NOT_FOUND";
    public static final @Fenum(FenumGroup.ERROR_CODE) String NOTIFICATION_DELIVERY_FAILED =
            "NOTIFICATION_DELIVERY_FAILED";

    // --- State Machine ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String INVALID_STATE_TRANSITION =
            "INVALID_STATE_TRANSITION";

    // --- System ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String INTERNAL_ERROR =
            "INTERNAL_ERROR";
    public static final @Fenum(FenumGroup.ERROR_CODE) String SERVICE_UNAVAILABLE =
            "SERVICE_UNAVAILABLE";

    // --- JSON ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String JSON_ERROR =
            "JSON_ERROR";

    // --- Events ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String EVENT_DESERIALIZATION_ERROR =
            "EVENT_DESERIALIZATION_ERROR";

    // --- Infrastructure ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String LOCK_ACQUISITION_FAILED =
            "LOCK_ACQUISITION_FAILED";

    // --- Internal API ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String INTERNAL_API_KEY_INVALID =
            "INTERNAL_API_KEY_INVALID";
    public static final @Fenum(FenumGroup.ERROR_CODE) String INTERNAL_IP_DENIED =
            "INTERNAL_IP_DENIED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String UNKNOWN_CALLBACK_TYPE =
            "UNKNOWN_CALLBACK_TYPE";

    // --- Outbox ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String OUTBOX_PUBLISH_FAILED =
            "OUTBOX_PUBLISH_FAILED";

    private ErrorCodes() {
    }
}
