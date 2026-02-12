package com.advertmarket.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.exception.ErrorCodes;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("ErrorCode catalog")
class ErrorCodeTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("HTTP status is in valid range")
    void httpStatus_inValidRange(ErrorCode code) {
        assertThat(code.httpStatus())
                .as("httpStatus for %s", code)
                .isBetween(400, 599);
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("Title key has correct format")
    void titleKey_hasCorrectFormat(ErrorCode code) {
        assertThat(code.titleKey())
                .startsWith("error.")
                .endsWith(".title")
                .contains(code.name());
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("Detail key has correct format")
    void detailKey_hasCorrectFormat(ErrorCode code) {
        assertThat(code.detailKey())
                .startsWith("error.")
                .endsWith(".detail")
                .contains(code.name());
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("Type URI has correct format")
    void typeUri_hasCorrectFormat(ErrorCode code) {
        assertThat(code.typeUri())
                .startsWith("urn:advertmarket:error:")
                .endsWith(code.name());
    }

    @Test
    @DisplayName("resolve() returns matching enum constant")
    void resolve_returnsMatchingConstant() {
        assertThat(ErrorCode.resolve("DEAL_NOT_FOUND"))
                .isEqualTo(ErrorCode.DEAL_NOT_FOUND);
    }

    @Test
    @DisplayName("resolve() returns null for unknown code")
    void resolve_returnsNullForUnknown() {
        assertThat(ErrorCode.resolve("NONEXISTENT_CODE"))
                .isNull();
    }

    @Test
    @DisplayName("Has expected number of error codes")
    void hasExpectedCount() {
        assertThat(ErrorCode.values()).hasSize(50);
    }

    @Test
    @DisplayName("Every ErrorCode enum has a matching ErrorCodes constant")
    void errorCodesParityWithEnum() {
        Set<String> enumNames = Arrays.stream(ErrorCode.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        Set<String> constantNames = Arrays.stream(
                        ErrorCodes.class.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers())
                        && Modifier.isStatic(f.getModifiers())
                        && Modifier.isFinal(f.getModifiers())
                        && f.getType() == String.class)
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(constantNames)
                .as("ErrorCodes constants must match ErrorCode enum 1:1")
                .isEqualTo(enumNames);
    }
}
