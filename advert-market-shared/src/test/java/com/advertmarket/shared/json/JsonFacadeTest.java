package com.advertmarket.shared.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JsonFacade")
class JsonFacadeTest {

    private record Sample(String name, int value) {
    }

    private JsonFacade json;

    @BeforeEach
    void setUp() {
        json = new JsonFacade(new ObjectMapper());
    }

    @Test
    @DisplayName("toBytes serializes to JSON byte array")
    void toBytes_serializesToBytes() {
        byte[] bytes = json.toBytes(new Sample("test", 42));
        var str = new String(bytes, StandardCharsets.UTF_8);

        assertThat(str).contains("\"name\":\"test\"");
        assertThat(str).contains("\"value\":42");
    }

    @Test
    @DisplayName("toJson serializes to JSON string")
    void toJson_serializesToString() {
        String result = json.toJson(new Sample("hello", 1));

        assertThat(result).contains("\"name\":\"hello\"");
    }

    @Test
    @DisplayName("fromBytes deserializes byte array")
    void fromBytes_deserializesBytes() {
        byte[] data = "{\"name\":\"abc\",\"value\":10}"
                .getBytes(StandardCharsets.UTF_8);

        Sample result = json.fromBytes(data, Sample.class);

        assertThat(result.name()).isEqualTo("abc");
        assertThat(result.value()).isEqualTo(10);
    }

    @Test
    @DisplayName("fromJson deserializes JSON string")
    void fromJson_deserializesString() {
        Sample result = json.fromJson(
                "{\"name\":\"x\",\"value\":5}", Sample.class);

        assertThat(result.name()).isEqualTo("x");
        assertThat(result.value()).isEqualTo(5);
    }

    @Test
    @DisplayName("readTree parses JSON string to tree")
    void readTree_parsesString() {
        var tree = json.readTree("{\"key\":\"val\"}");

        assertThat(tree.get("key").asText()).isEqualTo("val");
    }

    @Test
    @DisplayName("readTree parses byte array to tree")
    void readTree_parsesBytes() {
        byte[] data = "{\"k\":1}"
                .getBytes(StandardCharsets.UTF_8);

        var tree = json.readTree(data);

        assertThat(tree.get("k").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("treeToValue converts tree node to type")
    void treeToValue_convertsTreeToType() {
        var tree = json.readTree(
                "{\"name\":\"test\",\"value\":99}");

        Sample result = json.treeToValue(tree, Sample.class);

        assertThat(result).isEqualTo(new Sample("test", 99));
    }

    @Test
    @DisplayName("Malformed JSON throws JsonException")
    void malformedJson_throwsJsonException() {
        assertThatThrownBy(
                () -> json.fromJson("{invalid", Sample.class))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("Failed to deserialize");
    }

    @Test
    @DisplayName("JsonException carries JSON_ERROR code")
    void jsonException_carriesErrorCode() {
        assertThatThrownBy(
                () -> json.readTree("{broken"))
                .isInstanceOf(JsonException.class)
                .extracting("errorCode")
                .isEqualTo("JSON_ERROR");
    }

    @Test
    @DisplayName("typeFactory returns non-null factory")
    void typeFactory_returnsFactory() {
        assertThat(json.typeFactory()).isNotNull();
    }
}
