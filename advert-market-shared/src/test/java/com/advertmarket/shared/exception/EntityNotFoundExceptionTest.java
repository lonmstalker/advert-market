package com.advertmarket.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EntityNotFoundException")
class EntityNotFoundExceptionTest {

    @Test
    @DisplayName("Error code is derived from entity type")
    void errorCode_derivedFromEntityType() {
        var ex = new EntityNotFoundException(
                "Deal", "abc-123");

        assertThat(ex.getErrorCode())
                .isEqualTo("DEAL_NOT_FOUND");
        assertThat(ex.getMessage())
                .isEqualTo("Deal not found: abc-123");
        assertThat(ex.getEntityType()).isEqualTo("Deal");
        assertThat(ex.getEntityId()).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("Is a DomainException")
    void isDomainException() {
        var ex = new EntityNotFoundException("User", "42");
        assertThat(ex).isInstanceOf(DomainException.class);
    }
}
