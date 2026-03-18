package com.supermart.iot.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for JWT token generation, parsing, and validation.
 *
 * <p>Tokens are signed using HMAC-SHA and the expiration durations are
 * externally configured via {@code app.jwt.*} properties to avoid hardcoded
 * values. Access token expiration is set to 45 minutes per OWASP A07 and A05
 * recommendations (SCRUM-3).</p>
 */
@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    /**
     * Access token lifetime in milliseconds.
     * Configured to 2700000 ms (45 minutes) per SCRUM-3 security requirement.
     */
    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    /**
     * Generates a signed JWT access token for the given email subject.
     *
     * <p>The token includes a {@code type=access} claim and expires after
     * the duration configured in {@code app.jwt.access-token-expiration-ms}
     * (currently 45 minutes / 2700000 ms).</p>
     *
     * @param email the authenticated user's email address used as the JWT subject
     * @return a compact, signed JWT access token string
     */
    public String generateAccessToken(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email must not be null when generating an access token.");
        }
        log.debug("Generating access token for email={}", email);
        return buildToken(email, Map.of("type", "access"), accessTokenExpirationMs);
    }

    /**
     * Generates a signed JWT refresh token for the given email subject.
     *
     * <p>The token includes a {@code type=refresh} claim and expires after
     * the duration configured in {@code app.jwt.refresh-token-expiration-ms}.</p>
     *
     * @param email the authenticated user's email address used as the JWT subject
     * @return a compact, signed JWT refresh token string
     */
    public String generateRefreshToken(String email) {
        log.debug("Generating refresh token for email={}", email);
        return buildToken(email, Map.of("type", "refresh"), refreshTokenExpirationMs);
    }

    /**
     * Builds and signs a JWT token with the given subject, claims, and expiration.
     *
     * @param subject       the JWT subject (typically user email)
     * @param extraClaims   additional claims to embed in the token payload
     * @param expiration    token lifetime in milliseconds from the current time
     * @return a compact, signed JWT string
     */
    private String buildToken(String subject, Map<String, Object> extraClaims, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the email (subject claim) from the given JWT token.
     *
     * @param token the compact JWT string to parse
     * @return the email address stored as the JWT subject
     * @throws io.jsonwebtoken.JwtException if the token is malformed or the signature is invalid
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validates that the token belongs to the given user and has not expired.
     *
     * @param token       the compact JWT string to validate
     * @param userDetails the authenticated user whose credentials are checked
     * @return {@code true} if the token subject matches the user and the token is not expired;
     *         {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Checks whether the given JWT token has passed its expiration time.
     *
     * @param token the compact JWT string to inspect
     * @return {@code true} if the token expiration is before the current time; {@code false} otherwise
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from the given JWT token.
     *
     * @param token the compact JWT string to parse
     * @return the {@link Date} at which the token expires
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts an arbitrary claim from the JWT token using the provided resolver function.
     *
     * @param <T>            the type of the claim value
     * @param token          the compact JWT string to parse
     * @param claimsResolver a function that maps the full {@link Claims} object to the desired value
     * @return the extracted claim value
     * @throws io.jsonwebtoken.JwtException if the token is malformed or signature verification fails
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and verifies all claims from the given JWT token.
     *
     * @param token the compact JWT string to parse
     * @return the full {@link Claims} payload
     * @throws io.jsonwebtoken.JwtException if parsing fails or signature is invalid
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Derives the HMAC-SHA signing key from the Base64-encoded secret property.
     *
     * @return the {@link SecretKey} used to sign and verify JWT tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Returns the configured access token expiration duration in milliseconds.
     *
     * <p>Used by {@code AuthService} to populate the {@code expiresIn} field
     * of the login response (converted to seconds by the caller).</p>
     *
     * @return access token lifetime in milliseconds (currently 2700000 ms / 45 minutes)
     */
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
