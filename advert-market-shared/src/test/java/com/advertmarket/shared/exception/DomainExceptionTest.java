package com.advertmarket.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DomainException base class")
class DomainExceptionTest {

    @Test
    @DisplayName("Basic constructor sets errorCode and message")
    void basicConstructor_setsFields() {
        var ex = new DomainException(
                "ERR_001", "Something failed");

        assertThat(ex.getErrorCode()).isEqualTo("ERR_001");
        assertThat(ex.getMessage())
                .isEqualTo("Something failed");
        assertThat(ex.getContext()).isNull();
    }

    @Test
    @DisplayName("Context constructor creates immutable copy")
    void contextConstructor_createsImmutableCopy() {
        var context = new HashMap<String, Object>();
        context.put("key", "value");

        var ex = new DomainException(
                "ERR_002", "With context", context);

        assertThat(ex.getContext())
                .containsEntry("key", "value");

        context.put("key2", "value2");
        assertThat(ex.getContext())
                .doesNotContainKey("key2");
    }

    @Test
    @DisplayName("Cause constructor preserves the cause")
    void causeConstructor_preservesCause() {
        var cause = new RuntimeException("root cause");
        var ex = new DomainException(
                "ERR_003", "Caused", cause);

        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getContext()).isNull();
    }

    @Test
    @DisplayName("Null context is allowed")
    void nullContext_isAllowed() {
        var ex = new DomainException(
                "ERR_004", "No context",
                (Map<String, Object>) null);

        assertThat(ex.getContext()).isNull();
    }

    @Test
    @DisplayName("Null errorCode is rejected")
    void nullErrorCode_isRejected() {
        assertThatThrownBy(
                () -> new DomainException(null, "msg"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("errorCode");
    }
}
