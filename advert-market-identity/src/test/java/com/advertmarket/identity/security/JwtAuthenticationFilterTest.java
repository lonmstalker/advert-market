package com.advertmarket.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.identity.api.port.TokenBlacklistPort;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.model.UserBlockCheckPort;
import com.advertmarket.shared.model.UserId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("JwtAuthenticationFilter — Bearer token extraction and validation")
class JwtAuthenticationFilterTest {

    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistPort tokenBlacklistPort;
    private UserBlockCheckPort userBlockCheckPort;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        tokenBlacklistPort = mock(TokenBlacklistPort.class);
        userBlockCheckPort = mock(UserBlockCheckPort.class);
        filter = new JwtAuthenticationFilter(
                jwtTokenProvider, tokenBlacklistPort,
                userBlockCheckPort);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should set authentication for valid Bearer token")
    void shouldSetAuthenticationForValidToken() throws Exception {
        String token = "valid.jwt.token";
        TelegramAuthentication auth = new TelegramAuthentication(
                new UserId(42L), false, "jti-123", 0L);

        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + token);
        when(jwtTokenProvider.parseToken(token)).thenReturn(auth);
        when(tokenBlacklistPort.isBlacklisted("jti-123"))
                .thenReturn(false);
        when(userBlockCheckPort.isBlocked(42L))
                .thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isEqualTo(auth);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip when Authorization header is missing")
    void shouldSkipWhenHeaderMissing() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set auth when token is invalid")
    void shouldNotSetAuthWhenTokenInvalid() throws Exception {
        String token = "invalid.jwt.token";
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + token);
        when(jwtTokenProvider.parseToken(token))
                .thenThrow(new DomainException(
                        ErrorCodes.AUTH_INVALID_TOKEN, "bad token"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set auth when token is blacklisted")
    void shouldNotSetAuthWhenTokenBlacklisted() throws Exception {
        String token = "valid.jwt.token";
        TelegramAuthentication auth = new TelegramAuthentication(
                new UserId(42L), false, "jti-revoked", 0L);

        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + token);
        when(jwtTokenProvider.parseToken(token)).thenReturn(auth);
        when(tokenBlacklistPort.isBlacklisted("jti-revoked"))
                .thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set auth when user is blocked")
    void shouldNotSetAuthWhenUserBlocked() throws Exception {
        String token = "valid.jwt.token";
        TelegramAuthentication auth = new TelegramAuthentication(
                new UserId(99L), false, "jti-123", 0L);

        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + token);
        when(jwtTokenProvider.parseToken(token)).thenReturn(auth);
        when(tokenBlacklistPort.isBlacklisted("jti-123"))
                .thenReturn(false);
        when(userBlockCheckPort.isBlocked(99L))
                .thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
