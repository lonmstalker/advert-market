package com.advertmarket.identity.service;

import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.config.AuthProperties;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.json.JsonFacade;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Validates Telegram Mini App initData using HMAC-SHA256.
 *
 * <p>Not annotated as {@code @Component} — created as a {@code @Bean}
 * in the app module (needs bot token from communication module).
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "CT_CONSTRUCTOR_THROW",
        justification = "Bean constructor derives HMAC key")
public class TelegramInitDataValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String WEB_APP_DATA = "WebAppData";

    private final byte[] secretKey;
    private final int antiReplayWindowSeconds;
    private final JsonFacade jsonFacade;

    /**
     * Creates a validator.
     *
     * @param botToken   Telegram bot token
     * @param properties auth configuration
     * @param jsonFacade JSON serialization facade
     */
    public TelegramInitDataValidator(
            @NonNull String botToken,
            @NonNull AuthProperties properties,
            @NonNull JsonFacade jsonFacade) {
        this.secretKey = hmacSha256(
                WEB_APP_DATA.getBytes(StandardCharsets.UTF_8),
                botToken.getBytes(StandardCharsets.UTF_8));
        this.antiReplayWindowSeconds =
                properties.antiReplayWindowSeconds();
        this.jsonFacade = jsonFacade;
    }

    /**
     * Validates initData and extracts user information.
     *
     * @param initData raw Telegram initData query string
     * @return parsed Telegram user data
     * @throws DomainException if validation fails
     */
    @NonNull
    public TelegramUserData validate(@NonNull String initData) {
        Map<String, String> params = parseQueryString(initData);

        String receivedHash = params.remove("hash");
        if (receivedHash == null || receivedHash.isBlank()) {
            throw new DomainException(
                    "AUTH_INIT_DATA_INVALID",
                    "Missing hash in initData");
        }

        String dataCheckString = buildDataCheckString(params);
        byte[] computedHash = hmacSha256(
                secretKey,
                dataCheckString.getBytes(StandardCharsets.UTF_8));
        String computedHex = bytesToHex(computedHash);

        if (!MessageDigest.isEqual(
                computedHex.getBytes(StandardCharsets.UTF_8),
                receivedHash.getBytes(StandardCharsets.UTF_8))) {
            throw new DomainException(
                    "AUTH_INIT_DATA_INVALID",
                    "Invalid initData hash");
        }

        validateAuthDate(params);

        String userJson = params.get("user");
        if (userJson == null || userJson.isBlank()) {
            throw new DomainException(
                    "AUTH_INIT_DATA_INVALID",
                    "Missing user data in initData");
        }

        return jsonFacade.fromJson(userJson, TelegramUserData.class);
    }

    private void validateAuthDate(Map<String, String> params) {
        String authDateStr = params.get("auth_date");
        if (authDateStr == null) {
            throw new DomainException(
                    "AUTH_INIT_DATA_INVALID",
                    "Missing auth_date in initData");
        }

        long authDate;
        try {
            authDate = Long.parseLong(authDateStr);
        } catch (NumberFormatException e) {
            throw new DomainException(
                    "AUTH_INIT_DATA_INVALID",
                    "Invalid auth_date format");
        }

        long now = Instant.now().getEpochSecond();
        if (now - authDate > antiReplayWindowSeconds) {
            throw new DomainException(
                    "AUTH_INIT_DATA_INVALID",
                    "initData auth_date is too old");
        }
    }

    private static Map<String, String> parseQueryString(
            String queryString) {
        Map<String, String> params = new TreeMap<>();
        for (String pair : queryString.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) {
                continue;
            }
            String key = URLDecoder.decode(
                    pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(
                    pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    private static String buildDataCheckString(
            Map<String, String> sortedParams) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var entry : sortedParams.entrySet()) {
            if (!first) {
                sb.append('\n');
            }
            sb.append(entry.getKey())
                    .append('=')
                    .append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException
                 | InvalidKeyException e) {
            throw new IllegalStateException(
                    "HMAC-SHA256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
