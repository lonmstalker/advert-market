package com.advertmarket.app.error;

import com.advertmarket.shared.logging.MdcKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
 * HTML error pages.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String PROBLEM_JSON =
            "application/problem+json";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        log.debug("Authentication failed: {}",
                authException.getMessage());
        writeProblem(response, HttpStatus.UNAUTHORIZED,
                "AUTH_INVALID_TOKEN",
                "urn:advertmarket:error:AUTH_INVALID_TOKEN",
                "Authentication required",
                "Missing or invalid authentication token");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        log.debug("Access denied: {}",
                accessDeniedException.getMessage());
        writeProblem(response, HttpStatus.FORBIDDEN,
                "AUTH_INSUFFICIENT_PERMISSIONS",
                "urn:advertmarket:error:AUTH_INSUFFICIENT_PERMISSIONS",
                "Access denied",
                "You do not have permission to perform this action");
    }

    private void writeProblem(
            HttpServletResponse response,
            HttpStatus status,
            String errorCode,
            String typeUri,
            String title,
            String detail) throws IOException {
        var problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(typeUri));
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setProperty("error_code", errorCode);
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
