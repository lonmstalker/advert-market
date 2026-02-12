package com.advertmarket.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.identity.api.port.TokenBlacklistPort;
import com.advertmarket.shared.exception.DomainException;
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
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        tokenBlacklistPort = mock(TokenBlacklistPort.class);
        filter = new JwtAuthenticationFilter(
                jwtTokenProvider, tokenBlacklistPort);
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
                new UserId(42L), false, "jti-123");

        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + token);
        when(jwtTokenProvider.parseToken(token)).thenReturn(auth);
        when(tokenBlacklistPort.isBlacklisted("jti-123"))
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
    @DisplayName("Should not set auth when token is blacklisted")
    void shouldNotSetAuthWhenBlacklisted() throws Exception {
        String token = "blacklisted.jwt.token";
        TelegramAuthentication auth = new TelegramAuthentication(
                new UserId(42L), false, "jti-blacklisted");

        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + token);
        when(jwtTokenProvider.parseToken(token)).thenReturn(auth);
        when(tokenBlacklistPort.isBlacklisted("jti-blacklisted"))
                .thenReturn(true);

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
                        "AUTH_INVALID_TOKEN", "bad token"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
