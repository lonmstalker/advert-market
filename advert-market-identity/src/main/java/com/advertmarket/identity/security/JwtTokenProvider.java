package com.advertmarket.identity.security;

import com.advertmarket.identity.config.AuthProperties;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.model.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Generates and parses JWT tokens using JJWT (HS256).
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "CT_CONSTRUCTOR_THROW",
        justification = "Bean constructor validates configuration")
public class JwtTokenProvider {

    private static final String CLAIM_IS_OPERATOR = "is_operator";
    private static final String ISSUER = "advert-market";
    private static final String AUDIENCE = "advert-market-api";

    private final SecretKey signingKey;
    private final long expirationSeconds;

    /**
     * Creates a provider from auth properties.
     *
     * @param properties auth configuration
     */
    public JwtTokenProvider(@NonNull AuthProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(
                properties.jwt().secret()
                        .getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = properties.jwt().expiration();
    }

    /**
     * Generates a JWT for the given user.
     *
     * @param userId     user identifier
     * @param isOperator whether the user is an operator
     * @return signed JWT string
     */
    @NonNull
    public String generateToken(@NonNull UserId userId,
            boolean isOperator) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(String.valueOf(userId.value()))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusSeconds(expirationSeconds)))
                .claim(CLAIM_IS_OPERATOR, isOperator)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and validates a JWT token.
     *
     * @param token the JWT string
     * @return authentication object
     * @throws DomainException if the token is invalid or expired
     */
    @NonNull
    public TelegramAuthentication parseToken(@NonNull String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(ISSUER)
                    .requireAudience(AUDIENCE)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            long userId = Long.parseLong(claims.getSubject());
            boolean isOperator = Boolean.TRUE.equals(
                    claims.get(CLAIM_IS_OPERATOR, Boolean.class));
            String jti = claims.getId();

            return new TelegramAuthentication(
                    new UserId(userId), isOperator, jti);
        } catch (ExpiredJwtException e) {
            throw new DomainException(
                    "AUTH_TOKEN_EXPIRED",
                    "JWT token has expired");
        } catch (JwtException | IllegalArgumentException e) {
            throw new DomainException(
                    "AUTH_INVALID_TOKEN",
                    "Invalid JWT token");
        }
    }

    /** Returns the configured token lifetime in seconds. */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
