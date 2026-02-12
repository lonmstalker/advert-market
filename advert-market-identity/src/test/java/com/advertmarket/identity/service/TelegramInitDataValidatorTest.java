package com.advertmarket.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.config.AuthProperties;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.json.JsonFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TelegramInitDataValidator — HMAC-SHA256 initData validation")
class TelegramInitDataValidatorTest {

    private static final String BOT_TOKEN = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11";
    private static final long USER_ID = 987654321L;
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String USERNAME = "johndoe";
    private static final String LANGUAGE_CODE = "en";

    private TelegramInitDataValidator validator;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                new AuthProperties.Jwt("test-secret-minimum-32-bytes-long!!", 3600),
                300);
        JsonFacade jsonFacade = new JsonFacade(new ObjectMapper());
        validator = new TelegramInitDataValidator(BOT_TOKEN, properties, jsonFacade);
    }

    @Test
    @DisplayName("Should validate correct initData and parse user")
    void shouldValidateCorrectInitData() {
        String initData = buildValidInitData(Instant.now().getEpochSecond());

        TelegramUserData result = validator.validate(initData);

        assertThat(result.id()).isEqualTo(USER_ID);
        assertThat(result.firstName()).isEqualTo(FIRST_NAME);
        assertThat(result.lastName()).isEqualTo(LAST_NAME);
        assertThat(result.username()).isEqualTo(USERNAME);
        assertThat(result.languageCode()).isEqualTo(LANGUAGE_CODE);
    }

    @Test
    @DisplayName("Should parse user with optional fields missing")
    void shouldParseUserWithOptionalFieldsMissing() {
        String userJson = "{\"id\":" + USER_ID + ",\"first_name\":\"" + FIRST_NAME + "\"}";
        String initData = buildInitDataWithUser(
                userJson, Instant.now().getEpochSecond());

        TelegramUserData result = validator.validate(initData);

        assertThat(result.id()).isEqualTo(USER_ID);
        assertThat(result.firstName()).isEqualTo(FIRST_NAME);
        assertThat(result.lastName()).isNull();
        assertThat(result.username()).isNull();
    }

    @Test
    @DisplayName("Should reject initData with invalid hash")
    void shouldRejectInvalidHash() {
        String initData = buildValidInitData(Instant.now().getEpochSecond())
                .replaceFirst("hash=[^&]+", "hash=invalidhash");

        assertThatThrownBy(() -> validator.validate(initData))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("AUTH_INIT_DATA_INVALID");
    }

    @Test
    @DisplayName("Should reject initData with expired auth_date")
    void shouldRejectExpiredAuthDate() {
        long expiredAuthDate = Instant.now().getEpochSecond() - 600;
        String initData = buildValidInitData(expiredAuthDate);

        assertThatThrownBy(() -> validator.validate(initData))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("AUTH_INIT_DATA_INVALID");
    }

    @Test
    @DisplayName("Should reject initData without hash")
    void shouldRejectMissingHash() {
        String initData = "auth_date=" + Instant.now().getEpochSecond()
                + "&user=" + URLEncoder.encode("{\"id\":1,\"first_name\":\"X\"}",
                StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.validate(initData))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("AUTH_INIT_DATA_INVALID");
    }

    private String buildValidInitData(long authDate) {
        String userJson = String.format(
                "{\"id\":%d,\"first_name\":\"%s\",\"last_name\":\"%s\","
                        + "\"username\":\"%s\",\"language_code\":\"%s\"}",
                USER_ID, FIRST_NAME, LAST_NAME, USERNAME, LANGUAGE_CODE);
        return buildInitDataWithUser(userJson, authDate);
    }

    private String buildInitDataWithUser(String userJson, long authDate) {
        String encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8);
        String dataCheckString = "auth_date=" + authDate + "\n"
                + "user=" + userJson;

        byte[] secretKey = hmacSha256(
                "WebAppData".getBytes(StandardCharsets.UTF_8),
                BOT_TOKEN.getBytes(StandardCharsets.UTF_8));
        String hash = bytesToHex(hmacSha256(secretKey,
                dataCheckString.getBytes(StandardCharsets.UTF_8)));

        return "auth_date=" + authDate
                + "&user=" + encodedUser
                + "&hash=" + hash;
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
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
