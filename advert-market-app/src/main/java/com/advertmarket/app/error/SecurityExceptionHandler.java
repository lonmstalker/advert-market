package com.advertmarket.app.error;

import com.advertmarket.shared.error.ErrorCode;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.logging.MdcKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Unified handler for Spring Security 401/403 responses.
 *
 * <p>Returns RFC 9457 ProblemDetail JSON instead of the default
 * HTML error pages. Uses {@link ErrorCode} enum for error codes
 * and {@link LocalizationService} for i18n title/detail.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String PROBLEM_JSON =
            "application/problem+json";
    private static final String INTERNAL_PATH_PREFIX =
            "/internal/";

    private final ObjectMapper objectMapper;
    private final LocalizationService localization;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        log.debug("Authentication failed: {}",
                authException.getMessage());
        ErrorCode errorCode = isInternal(request)
                ? ErrorCode.INTERNAL_API_KEY_INVALID
                : ErrorCode.AUTH_INVALID_TOKEN;
        writeProblem(response, HttpStatus.UNAUTHORIZED,
                errorCode);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        log.debug("Access denied: {}",
                accessDeniedException.getMessage());
        ErrorCode errorCode = isInternal(request)
                ? ErrorCode.INTERNAL_IP_DENIED
                : ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS;
        writeProblem(response, HttpStatus.FORBIDDEN,
                errorCode);
    }

    private static boolean isInternal(
            HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith(INTERNAL_PATH_PREFIX);
    }

    private void writeProblem(
            HttpServletResponse response,
            HttpStatus status,
            ErrorCode errorCode) throws IOException {
        Locale locale = LocaleContextHolder.getLocale();
        var problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(errorCode.typeUri()));
        problem.setTitle(localization.msg(
                errorCode.titleKey(), locale));
        problem.setDetail(localization.msg(
                errorCode.detailKey(), locale));
        problem.setProperty("error_code", errorCode.name());
        problem.setProperty("timestamp",
                Instant.now().toString());

        var correlationId = MDC.get(
                MdcKeys.CORRELATION_ID);
        if (correlationId != null) {
            problem.setProperty("correlation_id",
                    correlationId);
        }

        response.setStatus(status.value());
        response.setContentType(PROBLEM_JSON);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(), problem);
    }
}
