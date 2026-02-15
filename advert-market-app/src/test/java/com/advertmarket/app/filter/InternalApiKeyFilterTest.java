package com.advertmarket.app.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.advertmarket.app.config.InternalApiProperties;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("InternalApiKeyFilter")
@ExtendWith(MockitoExtension.class)
class InternalApiKeyFilterTest {

    private static final String VALID_KEY = "test-api-key-12345";

    @Mock
    private MetricsFacade metrics;
    @Mock
    private FilterChain filterChain;

    private InternalApiKeyFilter createFilter(
            String apiKey, List<String> networks) {
        return new InternalApiKeyFilter(
                new InternalApiProperties(apiKey, networks),
                metrics);
    }

    @Test
    @DisplayName("Valid key and allowed IP passes")
    void validKeyAndAllowedIp_passes() throws Exception {
        var filter = createFilter(VALID_KEY,
                List.of("127.0.0.1/32"));
        var request = new MockHttpServletRequest();
        request.addHeader(InternalApiKeyFilter.HEADER, VALID_KEY);
        request.setRemoteAddr("127.0.0.1");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Missing key returns 401")
    void missingKey_returns401() throws Exception {
        var filter = createFilter(VALID_KEY,
                List.of("127.0.0.1/32"));
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        var response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilterInternal(
                request, response, filterChain))
                .isInstanceOf(BadCredentialsException.class);
        verify(filterChain, never()).doFilter(any(), any());
        verify(metrics).incrementCounter(
                eq(MetricNames.INTERNAL_AUTH_FAILED),
                eq("reason"), eq("invalid_key"));
    }

    @Test
    @DisplayName("Invalid key returns 401")
    void invalidKey_returns401() throws Exception {
        var filter = createFilter(VALID_KEY,
                List.of("127.0.0.1/32"));
        var request = new MockHttpServletRequest();
        request.addHeader(InternalApiKeyFilter.HEADER,
                "wrong-key");
        request.setRemoteAddr("127.0.0.1");
        var response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilterInternal(
                request, response, filterChain))
                .isInstanceOf(BadCredentialsException.class);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Denied IP returns 403")
    void deniedIp_returns403() throws Exception {
        var filter = createFilter(VALID_KEY,
                List.of("10.0.0.0/8"));
        var request = new MockHttpServletRequest();
        request.addHeader(InternalApiKeyFilter.HEADER, VALID_KEY);
        request.setRemoteAddr("192.168.1.1");
        var response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilterInternal(
                request, response, filterChain))
                .isInstanceOf(AccessDeniedException.class);
        verify(filterChain, never()).doFilter(any(), any());
        verify(metrics).incrementCounter(
                eq(MetricNames.INTERNAL_AUTH_FAILED),
                eq("reason"), eq("ip_denied"));
    }

    @Test
    @DisplayName("CIDR range includes subnet addresses")
    void cidrRange_includesSubnet() throws Exception {
        var filter = createFilter(VALID_KEY,
                List.of("172.18.0.0/16"));
        var request = new MockHttpServletRequest();
        request.addHeader(InternalApiKeyFilter.HEADER, VALID_KEY);
        request.setRemoteAddr("172.18.5.10");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Security context cleared after filter")
    void securityContext_clearedAfterFilter() throws Exception {
        var filter = createFilter(VALID_KEY,
                List.of("127.0.0.1/32"));
        var request = new MockHttpServletRequest();
        request.addHeader(InternalApiKeyFilter.HEADER, VALID_KEY);
        request.setRemoteAddr("127.0.0.1");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();
    }
}
