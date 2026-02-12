package com.advertmarket.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.model.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("AuthorizationService — operator check via SecurityContext")
class AuthorizationServiceTest {

    private final AuthorizationService service =
            new AuthorizationService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return true when user is operator")
    void shouldReturnTrueForOperator() {
        setAuth(true);
        assertThat(service.isOperator()).isTrue();
    }

    @Test
    @DisplayName("Should return false when user is not operator")
    void shouldReturnFalseForRegularUser() {
        setAuth(false);
        assertThat(service.isOperator()).isFalse();
    }

    @Test
    @DisplayName("Should throw when no authentication in context")
    void shouldThrowWhenNoAuthentication() {
        assertThatThrownBy(service::isOperator)
                .isInstanceOf(DomainException.class);
    }

    private void setAuth(boolean operator) {
        var auth = new TelegramAuthentication(
                new UserId(1L), operator, "jti", 0L);
        SecurityContextHolder.getContext()
                .setAuthentication(auth);
    }
}
