package com.advertmarket.shared.model;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * TON blockchain wallet address.
 *
 * <p>Supports both raw ({@code 0:...}) and user-friendly
 * ({@code UQ...}/{@code EQ...}) address formats.
 *
 * <p>User-friendly addresses are validated with CRC16-XMODEM
 * checksum verification per the TON address specification.
 */
public record TonAddress(@NonNull String value) {

    private static final Pattern RAW_FORMAT =
            Pattern.compile("^-?[0-9]+:[0-9a-fA-F]{64}$");
    private static final Pattern USER_FRIENDLY_FORMAT =
            Pattern.compile("^[A-Za-z0-9_-]{48}$");
    private static final int USER_FRIENDLY_BYTES = 36;
    private static final int CRC_OFFSET = 34;

    /**
     * Creates a TON address.
     *
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is blank or invalid
     */
    public TonAddress {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "TonAddress must not be blank");
        }
        if (RAW_FORMAT.matcher(value).matches()) {
            // Raw format — regex is sufficient
        } else if (USER_FRIENDLY_FORMAT.matcher(value).matches()) {
            validateUserFriendlyCrc(value);
        } else {
            throw new IllegalArgumentException(
                    "Invalid TON address format: " + value);
        }
    }

    private static void validateUserFriendlyCrc(String address) {
        byte[] decoded;
        try {
            decoded = Base64.getUrlDecoder().decode(address);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid TON address: base64url decode failed");
        }
        if (decoded.length != USER_FRIENDLY_BYTES) {
            throw new IllegalArgumentException(
                    "Invalid TON address: expected "
                            + USER_FRIENDLY_BYTES + " bytes, got "
                            + decoded.length);
        }

        byte[] data = Arrays.copyOf(decoded, CRC_OFFSET);
        int expected = ((decoded[CRC_OFFSET] & 0xFF) << 8)
                | (decoded[CRC_OFFSET + 1] & 0xFF);
        int actual = crc16Xmodem(data);

        if (actual != expected) {
            throw new IllegalArgumentException(
                    "Invalid TON address: CRC16 mismatch");
        }
    }

    @SuppressWarnings("MagicNumber")
    private static int crc16Xmodem(byte[] data) {
        int crc = 0x0000;
        for (byte b : data) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc = crc << 1;
                }
            }
        }
        return crc & 0xFFFF;
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}
