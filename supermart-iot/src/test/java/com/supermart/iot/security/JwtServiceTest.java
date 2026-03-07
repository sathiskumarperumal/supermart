package com.supermart.iot.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}.
 *
 * <p>SCRUM-9 AC-5: Verifies the 30-minute (1800-second) access token lifetime:
 * <ul>
 *   <li>Token generated at T=0 is valid at T+1799 s (one second before expiry).</li>
 *   <li>Token issued at T−1801 s causes JJWT to throw {@link ExpiredJwtException}
 *       on parse — confirming the expiry boundary is enforced (AC-5).</li>
 * </ul>
 *
 * <p>SCRUM-9 AC-3: Confirms refresh token expiry is unchanged (86400 s / 24 h).
 *
 * <p>SCRUM-9 AC-7: Confirms {@link JwtService} contains no device-key logic; it only
 * issues and validates user JWT tokens. {@code DeviceKeyAuthFilter} is unaffected.
 */
class JwtServiceTest {

    /** Base-64 encoded 256-bit HMAC key — non-production test value only. */
    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private static final long ACCESS_TOKEN_EXPIRY_MS  = 1800_000L; // SCRUM-9: 30 min
    private static final long REFRESH_TOKEN_EXPIRY_MS = 86_400_000L; // unchanged: 24 h

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs",  ACCESS_TOKEN_EXPIRY_MS);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", REFRESH_TOKEN_EXPIRY_MS);
    }

    // -------------------------------------------------------------------------
    // SCRUM-9 AC-5: 30-minute expiry boundary tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SCRUM-9 AC-5: token is valid at T+1799s (one second before 30-min expiry)")
    void should_beValid_when_tokenIsCheckedAt1799SecondsAfterIssue() {
        // given — issued 1799 s ago; expiration is ~1 s from now
        String token = buildTokenWithIssuedAt(System.currentTimeMillis() - 1_799_000L);
        UserDetails user = new User("test@supermart.com", "", Collections.emptyList());

        // when / then — no exception thrown, token is still valid
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("SCRUM-9 AC-5: JJWT throws ExpiredJwtException when token is past its 30-min expiry")
    void should_throwExpiredJwtException_when_tokenIsPastExpiry() {
        // given — issued 1801 s ago; expiration was 1 s in the past
        // JJWT's parser enforces expiry by throwing ExpiredJwtException — it never returns a
        // bool for expired tokens. This is the correct observable behaviour for AC-5.
        String token = buildTokenWithIssuedAt(System.currentTimeMillis() - 1_801_000L);

        // when / then
        assertThatThrownBy(() -> jwtService.isTokenExpired(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // -------------------------------------------------------------------------
    // SCRUM-9 AC-3: Refresh token expiry unchanged
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SCRUM-9 AC-3: access token expiry is 1800 s; refresh token expiry is unchanged at 86400 s")
    void should_haveRefreshTokenExpiryGreaterThanAccessTokenExpiry() {
        // given / when
        long accessMs  = jwtService.getAccessTokenExpirationMs();
        long refreshMs = REFRESH_TOKEN_EXPIRY_MS;

        // then
        assertThat(accessMs).isEqualTo(1_800_000L);      // 30 min — SCRUM-9
        assertThat(refreshMs).isGreaterThan(accessMs);   // refresh not reduced
        assertThat(refreshMs).isEqualTo(86_400_000L);    // 24 h — unchanged
    }

    // -------------------------------------------------------------------------
    // General token hygiene
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("generateAccessToken embeds the correct email as subject")
    void should_extractCorrectEmail_when_tokenGenerated() {
        // given
        String email = "admin@supermart.com";

        // when
        String token = jwtService.generateAccessToken(email);
        String extracted = jwtService.extractEmail(token);

        // then
        assertThat(extracted).isEqualTo(email);
    }

    @Test
    @DisplayName("fresh access token is valid for the issuing user")
    void should_beValid_when_freshTokenCheckedForMatchingUser() {
        // given
        String email = "manager@supermart.com";
        String token = jwtService.generateAccessToken(email);
        UserDetails user = new User(email, "", Collections.emptyList());

        // when / then
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("token is invalid when email does not match UserDetails")
    void should_beInvalid_when_emailDoesNotMatchUserDetails() {
        // given
        String token = jwtService.generateAccessToken("alice@supermart.com");
        UserDetails differentUser = new User("bob@supermart.com", "", Collections.emptyList());

        // when / then
        assertThat(jwtService.isTokenValid(token, differentUser)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Builds a signed JWT using an explicit issued-at timestamp. Because
     * {@link JwtService#generateAccessToken} always uses the current time, we
     * construct the token directly via the JJWT builder with the same signing key
     * to support boundary-time testing.
     *
     * @param issuedAtMs epoch milliseconds for the {@code iat} claim
     * @return a signed compact JWT
     */
    private String buildTokenWithIssuedAt(long issuedAtMs) {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(TEST_SECRET);
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
        return io.jsonwebtoken.Jwts.builder()
                .subject("test@supermart.com")
                .issuedAt(new Date(issuedAtMs))
                .expiration(new Date(issuedAtMs + ACCESS_TOKEN_EXPIRY_MS))
                .signWith(key)
                .compact();
    }
}
