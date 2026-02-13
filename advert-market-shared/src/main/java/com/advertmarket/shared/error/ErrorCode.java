package com.advertmarket.shared.error;

import com.advertmarket.shared.FenumGroup;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Catalog of machine-readable error codes with HTTP status mappings.
 *
 * <p>Used by the global exception handler to resolve
 * {@link com.advertmarket.shared.exception.DomainException#getErrorCode()}
 * strings into structured error responses.
 */
public enum ErrorCode {

    // --- Authentication ---
    AUTH_INVALID_TOKEN(401),
    AUTH_TOKEN_EXPIRED(401),
    AUTH_INIT_DATA_INVALID(401),
    AUTH_TOKEN_BLACKLISTED(401),
    AUTH_INSUFFICIENT_PERMISSIONS(403),
    USER_BLOCKED(403),
    ACCOUNT_DELETED(403),

    // --- Deal ---
    DEAL_NOT_FOUND(404),
    DEAL_ALREADY_EXISTS(409),
    DEAL_TERMS_MISMATCH(422),
    DEAL_DEADLINE_EXPIRED(410),
    DEAL_CANCELLED(410),

    // --- Financial ---
    INSUFFICIENT_BALANCE(422),
    DEPOSIT_TIMEOUT(408),
    DEPOSIT_AMOUNT_MISMATCH(422),
    DEPOSIT_REJECTED(422),
    PAYOUT_FAILED(502),
    REFUND_FAILED(502),
    LEDGER_INCONSISTENCY(500),
    COMMISSION_CALCULATION_ERROR(500),

    // --- Channel ---
    CHANNEL_NOT_FOUND(404),
    CHANNEL_ALREADY_REGISTERED(409),
    CHANNEL_INACCESSIBLE(502),
    CHANNEL_NOT_OWNED(403),
    CHANNEL_STATS_UNAVAILABLE(503),
    CHANNEL_BOT_NOT_MEMBER(403),
    CHANNEL_BOT_NOT_ADMIN(403),
    CHANNEL_BOT_INSUFFICIENT_RIGHTS(403),
    CHANNEL_USER_NOT_ADMIN(403),
    PRICING_RULE_NOT_FOUND(404),

    // --- Team ---
    TEAM_MEMBER_NOT_FOUND(404),
    TEAM_MEMBER_ALREADY_EXISTS(409),
    TEAM_LIMIT_EXCEEDED(422),
    TEAM_OWNER_PROTECTED(403),

    // --- Dispute ---
    DISPUTE_NOT_FOUND(404),
    DISPUTE_ALREADY_EXISTS(409),
    DISPUTE_RESOLUTION_FAILED(500),

    // --- Creative ---
    CREATIVE_NOT_FOUND(404),
    CREATIVE_INVALID_FORMAT(422),
    CREATIVE_TOO_LARGE(413),
    CREATIVE_REJECTED(422),

    // --- Delivery ---
    DELIVERY_VERIFICATION_FAILED(502),
    DELIVERY_POST_DELETED(410),
    DELIVERY_CONTENT_MODIFIED(409),
    DELIVERY_TIMEOUT(408),

    // --- Validation ---
    VALIDATION_FAILED(400),
    INVALID_PARAMETER(400),
    MISSING_REQUIRED_FIELD(400),

    // --- System ---
    INTERNAL_ERROR(500),
    SERVICE_UNAVAILABLE(503),
    RATE_LIMIT_EXCEEDED(429),

    // --- Compatibility with existing exception error codes ---
    INVALID_STATE_TRANSITION(409),
    ENTITY_NOT_FOUND(404),
    USER_NOT_FOUND(404),
    WALLET_NOT_FOUND(404),
    NOTIFICATION_DELIVERY_FAILED(502),

    // --- Infrastructure ---
    EVENT_DESERIALIZATION_ERROR(500),
    JSON_ERROR(500),
    LOCK_ACQUISITION_FAILED(409),
    OUTBOX_PUBLISH_FAILED(500),

    // --- Internal API ---
    INTERNAL_API_KEY_INVALID(401),
    INTERNAL_IP_DENIED(403),
    UNKNOWN_CALLBACK_TYPE(400);

    private static final Map<String, ErrorCode> LOOKUP =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(
                            Enum::name, Function.identity()));

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    /** Returns the HTTP status code for this error. */
    public int httpStatus() {
        return httpStatus;
    }

    /** Returns the i18n key for the error title. */
    @NonNull
    public String titleKey() {
        return "error." + name() + ".title";
    }

    /** Returns the i18n key for the error detail message. */
    @NonNull
    public String detailKey() {
        return "error." + name() + ".detail";
    }

    /** Returns the RFC 9457 type URI for this error. */
    @NonNull
    public String typeUri() {
        return "urn:advertmarket:error:" + name();
    }

    /**
     * Resolves a string error code to an {@link ErrorCode} enum.
     *
     * @param errorCode the error code string
     * @return the matching enum constant, or {@code null} if not found
     */
    @SuppressWarnings("fenum:argument")
    public static @Nullable ErrorCode resolve(
            @Fenum(FenumGroup.ERROR_CODE) @NonNull String errorCode) {
        Objects.requireNonNull(errorCode, "errorCode");
        return LOOKUP.get(errorCode);
    }
}
