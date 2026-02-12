package com.advertmarket.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.security.PrincipalAuthentication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TelegramAuthentication — PrincipalAuthentication contract")
class TelegramAuthenticationTest {

    private static final UserId USER_ID = new UserId(42L);
    private static final String JTI = "jwt-id-123";

    @Test
    @DisplayName("Should implement PrincipalAuthentication interface")
    void shouldImplementPrincipalAuthentication() {
        var auth = new TelegramAuthentication(USER_ID, false, JTI, 0L);
        assertThat(auth).isInstanceOf(PrincipalAuthentication.class);
    }

    @Test
    @DisplayName("Should return userId via getUserId()")
    void shouldReturnUserId() {
        var auth = new TelegramAuthentication(USER_ID, false, JTI, 0L);
        assertThat(auth.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Should return jti via getJti()")
    void shouldReturnJti() {
        var auth = new TelegramAuthentication(USER_ID, false, JTI, 0L);
        assertThat(auth.getJti()).isEqualTo(JTI);
    }

    @Test
    @DisplayName("Should return operator flag")
    void shouldReturnOperatorFlag() {
        var operator = new TelegramAuthentication(USER_ID, true, JTI, 0L);
        var regular = new TelegramAuthentication(USER_ID, false, JTI, 0L);

        assertThat(operator.isOperator()).isTrue();
        assertThat(regular.isOperator()).isFalse();
    }

    @Test
    @DisplayName("Should always be authenticated")
    void shouldAlwaysBeAuthenticated() {
        var auth = new TelegramAuthentication(USER_ID, false, JTI, 0L);
        assertThat(auth.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("Should reject setAuthenticated()")
    void shouldRejectSetAuthenticated() {
        var auth = new TelegramAuthentication(USER_ID, false, JTI, 0L);
        assertThatThrownBy(() -> auth.setAuthenticated(false))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should return userId as principal")
    void shouldReturnUserIdAsPrincipal() {
        var auth = new TelegramAuthentication(USER_ID, false, JTI, 0L);
        assertThat(auth.getPrincipal()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Should return user ID string as name")
    void shouldReturnNameAsString() {
        var auth = new TelegramAuthentication(USER_ID, false, JTI, 0L);
        assertThat(auth.getName()).isEqualTo("42");
    }

    @Test
    @DisplayName("Should return empty authorities")
    void shouldReturnEmptyAuthorities() {
        var auth = new TelegramAuthentication(USER_ID, false, JTI, 0L);
        assertThat(auth.getAuthorities()).isEmpty();
    }
}
