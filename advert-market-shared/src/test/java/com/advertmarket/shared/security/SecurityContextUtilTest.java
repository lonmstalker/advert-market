package com.advertmarket.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.model.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

@DisplayName("SecurityContextUtil principal extraction")
class SecurityContextUtilTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Returns PrincipalAuthentication when present and authenticated")
    void currentAuthentication_returnsPrincipal() {
        var principal = mockPrincipal(
                new UserId(42L), "jti-abc", false);
        setAuthentication(principal);

        assertThat(SecurityContextUtil.currentAuthentication())
                .isSameAs(principal);
    }

    @Test
    @DisplayName("Throws DomainException when no authentication")
    void currentAuthentication_noAuth_throwsDomainException() {
        assertThatThrownBy(SecurityContextUtil::currentAuthentication)
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(
                        ((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCodes.AUTH_INVALID_TOKEN));
    }

    @Test
    @DisplayName("Throws DomainException for non-PrincipalAuthentication")
    void currentAuthentication_wrongType_throwsDomainException() {
        var context = new SecurityContextImpl();
        context.setAuthentication(
                mock(org.springframework.security.core.Authentication.class));
        SecurityContextHolder.setContext(context);

        assertThatThrownBy(SecurityContextUtil::currentAuthentication)
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(
                        ((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCodes.AUTH_INVALID_TOKEN));
    }

    @Test
    @DisplayName("Throws DomainException when PrincipalAuthentication is not authenticated")
    void currentAuthentication_notAuthenticated_throwsDomainException() {
        var auth = mock(PrincipalAuthentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        var context = new SecurityContextImpl();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        assertThatThrownBy(SecurityContextUtil::currentAuthentication)
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(
                        ((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCodes.AUTH_INVALID_TOKEN));
    }

    @Test
    @DisplayName("currentUserId returns the user identifier")
    void currentUserId_returnsUserId() {
        var userId = new UserId(99L);
        setAuthentication(mockPrincipal(userId, "jti-1", false));

        assertThat(SecurityContextUtil.currentUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("currentJti returns the token identifier")
    void currentJti_returnsJti() {
        setAuthentication(
                mockPrincipal(new UserId(1L), "jti-xyz", false));

        assertThat(SecurityContextUtil.currentJti())
                .isEqualTo("jti-xyz");
    }

    @Test
    @DisplayName("isOperator returns true for operator users")
    void isOperator_returnsTrueForOperator() {
        setAuthentication(
                mockPrincipal(new UserId(1L), "jti-1", true));

        assertThat(SecurityContextUtil.isOperator()).isTrue();
    }

    @Test
    @DisplayName("isOperator returns false for regular users")
    void isOperator_returnsFalseForRegularUser() {
        setAuthentication(
                mockPrincipal(new UserId(1L), "jti-1", false));

        assertThat(SecurityContextUtil.isOperator()).isFalse();
    }

    private static PrincipalAuthentication mockPrincipal(
            UserId userId, String jti, boolean operator) {
        var auth = mock(PrincipalAuthentication.class);
        when(auth.getUserId()).thenReturn(userId);
        when(auth.getJti()).thenReturn(jti);
        when(auth.isOperator()).thenReturn(operator);
        when(auth.isAuthenticated()).thenReturn(true);
        return auth;
    }

    private static void setAuthentication(
            PrincipalAuthentication auth) {
        var context = new SecurityContextImpl();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}
