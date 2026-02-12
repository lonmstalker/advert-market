package com.advertmarket.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidStateTransitionException")
class InvalidStateTransitionExceptionTest {

    @Test
    @DisplayName("Contains entity type, from and to states")
    void containsTransitionDetails() {
        var ex = new InvalidStateTransitionException(
                "Deal", "DRAFT", "FUNDED");

        assertThat(ex.getErrorCode())
                .isEqualTo("INVALID_STATE_TRANSITION");
        assertThat(ex.getMessage())
                .contains("Deal", "DRAFT", "FUNDED");
        assertThat(ex.getEntityType()).isEqualTo("Deal");
        assertThat(ex.getFrom()).isEqualTo("DRAFT");
        assertThat(ex.getTo()).isEqualTo("FUNDED");
    }

    @Test
    @DisplayName("Is a DomainException")
    void isDomainException() {
        var ex = new InvalidStateTransitionException(
                "Deal", "DRAFT", "FUNDED");
        assertThat(ex).isInstanceOf(DomainException.class);
    }
}
