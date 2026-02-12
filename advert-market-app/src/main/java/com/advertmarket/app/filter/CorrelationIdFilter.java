package com.advertmarket.app.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that propagates or generates a correlation ID.
 *
 * <p>Reads {@code X-Correlation-Id} from the request header.
 * If absent, generates a new UUID. The ID is stored in MDC
 * for structured logging and added to the response header.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** HTTP header name for the correlation ID. */
    public static final String HEADER = "X-Correlation-Id";

    /** MDC key for the correlation ID. */
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = sanitize(
                request.getHeader(HEADER));

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID().toString();
        }
    }
}
