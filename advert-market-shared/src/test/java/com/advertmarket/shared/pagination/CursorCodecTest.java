package com.advertmarket.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CursorCodec for keyset pagination")
class CursorCodecTest {

    @Test
    @DisplayName("Encode and decode round-trip")
    void encodeAndDecode_roundTrip() {
        var fields = Map.of(
                "id", "42", "created", "2026-01-01");
        String cursor = CursorCodec.encode(fields);
        Map<String, String> decoded =
                CursorCodec.decode(cursor);

        assertThat(decoded).containsAllEntriesOf(fields);
    }

    @Test
    @DisplayName("Empty map encodes to empty string")
    void emptyMap_encodesToEmptyString() {
        assertThat(CursorCodec.encode(Map.of())).isEmpty();
    }

    @Test
    @DisplayName("Empty string decodes to empty map")
    void emptyString_decodesToEmptyMap() {
        assertThat(CursorCodec.decode("")).isEmpty();
    }

    @Test
    @DisplayName("Special characters are preserved")
    void specialCharacters_arePreserved() {
        var fields = Map.of(
                "name", "hello world",
                "sym", "a=b&c=d");
        String cursor = CursorCodec.encode(fields);
        Map<String, String> decoded =
                CursorCodec.decode(cursor);

        assertThat(decoded).containsAllEntriesOf(fields);
    }

    @Test
    @DisplayName("Cursor is URL-safe Base64")
    void cursor_isUrlSafeBase64() {
        String cursor = CursorCodec.encode(
                Map.of("key", "value"));
        assertThat(cursor).matches("[A-Za-z0-9_-]*");
    }

    @Test
    @DisplayName("Encoding is deterministic")
    void encoding_isDeterministic() {
        var fields = Map.of("b", "2", "a", "1");
        String first = CursorCodec.encode(fields);
        String second = CursorCodec.encode(fields);
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("Malformed Base64 throws IllegalArgumentException")
    void malformedBase64_throwsException() {
        assertThatThrownBy(
                () -> CursorCodec.decode("!!!not-base64!!!"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
