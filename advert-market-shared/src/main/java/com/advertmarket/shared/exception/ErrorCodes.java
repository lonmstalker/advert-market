package com.advertmarket.shared.exception;

import com.advertmarket.shared.FenumGroup;
import org.checkerframework.checker.fenum.qual.Fenum;

/**
 * Centralized error code constants for {@link DomainException}.
 */
public final class ErrorCodes {

    // --- Auth ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String AUTH_INIT_DATA_INVALID =
            "AUTH_INIT_DATA_INVALID";
    public static final @Fenum(FenumGroup.ERROR_CODE) String AUTH_TOKEN_EXPIRED =
            "AUTH_TOKEN_EXPIRED";
    public static final @Fenum(FenumGroup.ERROR_CODE) String AUTH_INVALID_TOKEN =
            "AUTH_INVALID_TOKEN";

    // --- Rate Limiting ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String RATE_LIMIT_EXCEEDED =
            "RATE_LIMIT_EXCEEDED";

    // --- Entities ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String ENTITY_NOT_FOUND =
            "ENTITY_NOT_FOUND";

    // --- JSON ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String JSON_ERROR =
            "JSON_ERROR";

    // --- Events ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String EVENT_DESERIALIZATION_ERROR =
            "EVENT_DESERIALIZATION_ERROR";

    // --- State Machine ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String INVALID_STATE_TRANSITION =
            "INVALID_STATE_TRANSITION";

    // --- Financial ---
    public static final @Fenum(FenumGroup.ERROR_CODE) String INSUFFICIENT_BALANCE =
            "INSUFFICIENT_BALANCE";

    private ErrorCodes() {
    }
}
