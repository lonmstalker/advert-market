package com.advertmarket.app.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.advertmarket.shared.error.ErrorCode;
import com.advertmarket.shared.i18n.LocalizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

@DisplayName("SecurityExceptionHandler — 401/403 JSON responses")
class SecurityExceptionHandlerTest {

    private SecurityExceptionHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LocalizationService localization =
            mock(LocalizationService.class);

    @BeforeEach
    void setUp() {
        when(localization.msg(any(String.class), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        handler = new SecurityExceptionHandler(
                objectMapper, localization);
    }

    @Test
    @DisplayName("Should return 401 with problem+json on authentication failure")
    @SuppressWarnings("unchecked")
    void shouldReturn401OnAuthenticationFailure() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        handler.commence(request, response,
                new BadCredentialsException("Bad token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType())
                .startsWith("application/problem+json");

        var body = objectMapper.readValue(
                response.getContentAsString(), Map.class);
        assertThat(body).containsEntry("status", 401);

        var props = (Map<String, Object>) body.get("properties");
        assertThat(props).containsEntry(
                "error_code", "AUTH_INVALID_TOKEN");
        assertThat(props).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should return 403 with problem+json on access denied")
    @SuppressWarnings("unchecked")
    void shouldReturn403OnAccessDenied() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        handler.handle(request, response,
                new AccessDeniedException("Forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType())
                .startsWith("application/problem+json");

        var body = objectMapper.readValue(
                response.getContentAsString(), Map.class);
        assertThat(body).containsEntry("status", 403);

        var props = (Map<String, Object>) body.get("properties");
        assertThat(props).containsEntry(
                "error_code", "AUTH_INSUFFICIENT_PERMISSIONS");
        assertThat(props).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should include type URI in 401 response")
    @SuppressWarnings("unchecked")
    void shouldIncludeTypeUriIn401() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        handler.commence(request, response,
                new BadCredentialsException("Bad"));

        var body = objectMapper.readValue(
                response.getContentAsString(), Map.class);
        assertThat(body).containsEntry(
                "type", "urn:advertmarket:error:AUTH_INVALID_TOKEN");
    }

    @Test
    @DisplayName("Should include type URI in 403 response")
    @SuppressWarnings("unchecked")
    void shouldIncludeTypeUriIn403() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        handler.handle(request, response,
                new AccessDeniedException("Denied"));

        var body = objectMapper.readValue(
                response.getContentAsString(), Map.class);
        assertThat(body).containsEntry(
                "type",
                "urn:advertmarket:error:AUTH_INSUFFICIENT_PERMISSIONS");
    }

    @Test
    @DisplayName("Should use localized title and detail")
    @SuppressWarnings("unchecked")
    void shouldUseLocalizedMessages() throws Exception {
        when(localization.msg(
                eq(ErrorCode.AUTH_INVALID_TOKEN.titleKey()),
                any(Locale.class)))
                .thenReturn("Localized Title");
        when(localization.msg(
                eq(ErrorCode.AUTH_INVALID_TOKEN.detailKey()),
                any(Locale.class)))
                .thenReturn("Localized Detail");

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        handler.commence(request, response,
                new BadCredentialsException("Bad"));

        var body = objectMapper.readValue(
                response.getContentAsString(), Map.class);
        assertThat(body).containsEntry("title", "Localized Title");
        assertThat(body).containsEntry("detail", "Localized Detail");
    }
}
