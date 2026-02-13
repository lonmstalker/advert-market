package com.advertmarket.app.filter;

import com.advertmarket.app.config.InternalApiProperties;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that authenticates internal API requests via API key and IP whitelist.
 *
 * <p>Checks the {@code X-Internal-Api-Key} header using constant-time
 * comparison and validates the client IP against allowed CIDR networks.
 */
@Slf4j
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    /** HTTP header name for the internal API key. */
    public static final String HEADER = "X-Internal-Api-Key";

    private final InternalApiProperties properties;
    private final MetricsFacade metrics;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        var apiKey = request.getHeader(HEADER);
        if (apiKey == null || !constantTimeEquals(
                apiKey, properties.apiKey())) {
            log.warn("Internal API key invalid from IP: {}",
                    request.getRemoteAddr());
            metrics.incrementCounter(MetricNames.INTERNAL_AUTH_FAILED,
                    "reason", "invalid_key");
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        if (!isIpAllowed(request.getRemoteAddr())) {
            log.warn("Internal API request from denied IP: {}",
                    request.getRemoteAddr());
            metrics.incrementCounter(MetricNames.INTERNAL_AUTH_FAILED,
                    "reason", "ip_denied");
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(
                new InternalServiceAuthentication());
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }

    private boolean isIpAllowed(String remoteAddr) {
        try {
            var clientAddr = InetAddress.getByName(remoteAddr);
            for (var cidr : properties.allowedNetworks()) {
                if (isInCidr(clientAddr, cidr)) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            log.warn("Failed to resolve IP address: {}",
                    remoteAddr);
        }
        return false;
    }

    private static boolean isInCidr(
            InetAddress addr, String cidr) {
        try {
            var parts = cidr.split("/");
            var networkAddr = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);

            byte[] addrBytes = addr.getAddress();
            byte[] networkBytes = networkAddr.getAddress();
            if (addrBytes.length != networkBytes.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (addrBytes[i] != networkBytes[i]) {
                    return false;
                }
            }

            if (remainingBits > 0 && fullBytes < addrBytes.length) {
                int mask = 0xFF << (8 - remainingBits);
                return (addrBytes[fullBytes] & mask)
                        == (networkBytes[fullBytes] & mask);
            }

            return true;
        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("Invalid CIDR notation: {}", cidr);
            return false;
        }
    }
}
