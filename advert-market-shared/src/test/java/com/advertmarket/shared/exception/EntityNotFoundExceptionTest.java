package com.advertmarket.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EntityNotFoundException")
class EntityNotFoundExceptionTest {

    @Test
    @DisplayName("Uses explicit error code from ErrorCodes catalog")
    void errorCode_fromCatalog() {
        var ex = new EntityNotFoundException(
                ErrorCodes.DEAL_NOT_FOUND, "Deal", "abc-123");

        assertThat(ex.getErrorCode())
                .isEqualTo("DEAL_NOT_FOUND");
        assertThat(ex.getMessage())
                .isEqualTo("Deal not found: abc-123");
        assertThat(ex.getEntityType()).isEqualTo("Deal");
        assertThat(ex.getEntityId()).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("Generic ENTITY_NOT_FOUND code works")
    void entityNotFound_genericCode() {
        var ex = new EntityNotFoundException(
                ErrorCodes.ENTITY_NOT_FOUND, "Widget", "99");

        assertThat(ex.getErrorCode())
                .isEqualTo("ENTITY_NOT_FOUND");
        assertThat(ex.getMessage())
                .isEqualTo("Widget not found: 99");
    }

    @Test
    @DisplayName("Is a DomainException")
    void isDomainException() {
        var ex = new EntityNotFoundException(
                ErrorCodes.USER_NOT_FOUND, "User", "42");
        assertThat(ex).isInstanceOf(DomainException.class);
    }
}
