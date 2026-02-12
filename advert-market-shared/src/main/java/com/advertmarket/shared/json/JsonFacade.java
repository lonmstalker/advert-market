package com.advertmarket.shared.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.IOException;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Facade over Jackson {@link ObjectMapper} that converts checked
 * exceptions into unchecked {@link JsonException}.
 *
 * <p>Eliminates the need for try-catch blocks around every
 * Jackson call. All methods throw {@link JsonException} on failure.
 */
public class JsonFacade {

    private final ObjectMapper objectMapper;

    /**
     * Creates a facade wrapping the given mapper.
     *
     * @param objectMapper Jackson object mapper
     */
    public JsonFacade(@NonNull ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper, "objectMapper");
    }

    /**
     * Serializes a value to JSON bytes.
     *
     * @param value the value to serialize
     * @return JSON byte array
     */
    public byte[] toBytes(@NonNull Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new JsonException(
                    "Failed to serialize to bytes", e);
        }
    }

    /**
     * Serializes a value to a JSON string.
     *
     * @param value the value to serialize
     * @return JSON string
     */
    @NonNull
    public String toJson(@NonNull Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new JsonException(
                    "Failed to serialize to JSON string", e);
        }
    }

    /**
     * Deserializes JSON bytes into the given type.
     *
     * @param data JSON byte array
     * @param type target class
     * @param <T> target type
     * @return deserialized object
     */
    @NonNull
    public <T> T fromBytes(byte @NonNull [] data,
            @NonNull Class<T> type) {
        try {
            return objectMapper.readValue(data, type);
        } catch (IOException e) {
            throw new JsonException(
                    "Failed to deserialize from bytes", e);
        }
    }

    /**
     * Deserializes a JSON string into the given type.
     *
     * @param json JSON string
     * @param type target class
     * @param <T> target type
     * @return deserialized object
     */
    @NonNull
    public <T> T fromJson(@NonNull String json,
            @NonNull Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new JsonException(
                    "Failed to deserialize from JSON string", e);
        }
    }

    /**
     * Parses JSON bytes into a tree.
     *
     * @param data JSON byte array
     * @return root node of the parsed tree
     */
    @NonNull
    public JsonNode readTree(byte @NonNull [] data) {
        try {
            return objectMapper.readTree(data);
        } catch (IOException e) {
            throw new JsonException(
                    "Failed to parse JSON tree from bytes", e);
        }
    }

    /**
     * Parses a JSON string into a tree.
     *
     * @param json JSON string
     * @return root node of the parsed tree
     */
    @NonNull
    public JsonNode readTree(@NonNull String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            throw new JsonException(
                    "Failed to parse JSON tree from string", e);
        }
    }

    /**
     * Converts a JSON tree node to the given type.
     *
     * @param tree JSON tree node
     * @param type target class
     * @param <T> target type
     * @return deserialized object
     */
    @NonNull
    public <T> T treeToValue(@NonNull JsonNode tree,
            @NonNull Class<T> type) {
        try {
            return objectMapper.treeToValue(tree, type);
        } catch (IOException e) {
            throw new JsonException(
                    "Failed to convert tree to "
                            + type.getSimpleName(), e);
        }
    }

    /**
     * Converts a value (including {@link JsonNode}) to a target
     * type described by a {@link JavaType}.
     *
     * @param fromValue source value (typically a JsonNode tree)
     * @param toType target JavaType (may be parametric)
     * @param <T> target type
     * @return the converted object
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public <T> T convertValue(@NonNull Object fromValue,
            @NonNull JavaType toType) {
        try {
            return (T) objectMapper.convertValue(
                    fromValue, toType);
        } catch (IllegalArgumentException e) {
            throw new JsonException(
                    "Failed to convert value to "
                            + toType, e);
        }
    }

    /** Returns the type factory for constructing generic types. */
    @NonNull
    public TypeFactory typeFactory() {
        return objectMapper.getTypeFactory();
    }
}
