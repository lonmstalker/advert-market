package com.advertmarket.shared.pagination;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Encodes and decodes cursor tokens for keyset pagination.
 *
 * <p>Cursors are represented as Base64 URL-safe encoded
 * query strings ({@code key=value&key2=value2}).
 */
public final class CursorCodec {

    private static final int MAX_CURSOR_LENGTH = 1024;

    private CursorCodec() {
    }

    /**
     * Encodes fields into a cursor token.
     *
     * @param fields the cursor fields
     * @return Base64 URL-safe encoded cursor
     */
    public static @NonNull String encode(
            @NonNull Map<String, String> fields) {
        if (fields.isEmpty()) {
            return "";
        }
        var sorted = new TreeMap<>(fields);
        var sb = new StringBuilder();
        sorted.forEach((key, value) -> {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(
                    key, StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(
                    value, StandardCharsets.UTF_8));
        });
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        sb.toString()
                                .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a cursor token into fields.
     *
     * @param cursor the Base64 URL-safe encoded cursor
     * @return decoded cursor fields
     */
    public static @NonNull Map<String, String> decode(
            @NonNull String cursor) {
        if (cursor.isEmpty()) {
            return Map.of();
        }
        if (cursor.length() > MAX_CURSOR_LENGTH) {
            throw new IllegalArgumentException(
                    "Cursor exceeds max length: "
                            + cursor.length());
        }
        byte[] bytes = Base64.getUrlDecoder().decode(cursor);
        String queryString =
                new String(bytes, StandardCharsets.UTF_8);
        var result = new LinkedHashMap<String, String>();
        for (String pair : queryString.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8);
            String val = parts.length > 1
                    ? URLDecoder.decode(
                            parts[1], StandardCharsets.UTF_8)
                    : "";
            result.put(key, val);
        }
        return Collections.unmodifiableMap(result);
    }
}
