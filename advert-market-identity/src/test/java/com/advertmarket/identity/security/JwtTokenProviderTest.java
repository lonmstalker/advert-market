package com.advertmarket.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advertmarket.identity.config.AuthProperties;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtTokenProvider — JWT token generation and parsing")
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                new AuthProperties.Jwt(
                        "test-secret-minimum-32-bytes-long!!",
                        3600),
                300);
        provider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("Should generate and parse token in a roundtrip")
    void shouldGenerateAndParseToken() {
        UserId userId = new UserId(123456789L);

        String token = provider.generateToken(userId, false);
        TelegramAuthentication auth = provider.parseToken(token);

        assertThat(auth.getUserId()).isEqualTo(userId);
        assertThat(auth.isOperator()).isFalse();
        assertThat(auth.getJti()).isNotBlank();
        assertThat(auth.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("Should preserve operator flag in claims")
    void shouldPreserveOperatorFlag() {
        UserId userId = new UserId(42L);

        String token = provider.generateToken(userId, true);
        TelegramAuthentication auth = provider.parseToken(token);

        assertThat(auth.isOperator()).isTrue();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        AuthProperties shortLived = new AuthProperties(
                new AuthProperties.Jwt(
                        "test-secret-minimum-32-bytes-long!!",
                        -1),
                300);
        JwtTokenProvider shortProvider =
                new JwtTokenProvider(shortLived);

        String token = shortProvider.generateToken(
                new UserId(1L), false);

        assertThatThrownBy(() -> provider.parseToken(token))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("AUTH_TOKEN_EXPIRED");
    }

    @Test
    @DisplayName("Should reject tampered token")
    void shouldRejectTamperedToken() {
        String token = provider.generateToken(
                new UserId(1L), false);
        String tampered = token + "x";

        assertThatThrownBy(() -> provider.parseToken(tampered))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("AUTH_INVALID_TOKEN");
    }

    @Test
    @DisplayName("Should return configured expiration seconds")
    void shouldReturnExpirationSeconds() {
        assertThat(provider.getExpirationSeconds()).isEqualTo(3600);
    }
}
