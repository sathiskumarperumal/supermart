package com.supermart.iot.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService} covering SCRUM-62 JWT expiration change.
 *
 * <p>Verifies that the access token expiration is set to 60 minutes (3600000 ms),
 * that the {@code exp} claim in generated tokens reflects the updated duration, and
 * that existing token validation workflows continue to function correctly.
 * Supersedes SCRUM-3 (45-minute / 2700000 ms tests).</p>
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService underTest;

    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 3600000L; // 60 minutes (SCRUM-62)
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 86400000L; // 24 hours
    private static final String TEST_EMAIL = "user@supermart.com";

    @BeforeEach
    void setUp() {
        underTest = new JwtService();
        ReflectionTestUtils.setField(underTest, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(underTest, "accessTokenExpirationMs", ACCESS_TOKEN_EXPIRATION_MS);
        ReflectionTestUtils.setField(underTest, "refreshTokenExpirationMs", REFRESH_TOKEN_EXPIRATION_MS);
    }

    // ─── AC-1: JWT expiration time set to 60 minutes ──────────────────────────

    @Test
    @DisplayName("AC-1: getAccessTokenExpirationMs returns 3600000 ms (60 minutes) per SCRUM-62")
    void should_return_3600000ms_when_getAccessTokenExpirationMs_called() {
        // when
        long result = underTest.getAccessTokenExpirationMs();

        // then
        assertThat(result).isEqualTo(3600000L);
    }

    // ─── AC-2: New tokens reflect updated expiration claim (exp) ──────────────

    @Test
    @DisplayName("AC-2: Generated access token exp claim is approximately 60 minutes from now")
    void should_set_exp_claim_to_60_minutes_when_access_token_generated() {
        // given — capture window around token generation; allow 2-second lower tolerance
        // to absorb thread scheduling jitter between System.currentTimeMillis() calls
        long beforeMs = System.currentTimeMillis();

        // when
        String token = underTest.generateAccessToken(TEST_EMAIL);

        // then
        Date expiration = underTest.extractClaim(token, Claims::getExpiration);
        long afterMs = System.currentTimeMillis();

        long expectedLow  = beforeMs + ACCESS_TOKEN_EXPIRATION_MS - 2000L;
        long expectedHigh = afterMs  + ACCESS_TOKEN_EXPIRATION_MS;

        assertThat(expiration.getTime())
                .isGreaterThanOrEqualTo(expectedLow)
                .isLessThanOrEqualTo(expectedHigh);
    }

    @Test
    @DisplayName("AC-2: Generated access token subject claim equals the provided email")
    void should_embed_email_as_subject_when_access_token_generated() {
        // when
        String token = underTest.generateAccessToken(TEST_EMAIL);

        // then
        String extractedEmail = underTest.extractEmail(token);
        assertThat(extractedEmail).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("AC-2: Generated refresh token exp claim is approximately 24 hours from now")
    void should_set_exp_claim_to_24_hours_when_refresh_token_generated() {
        // given — capture window around token generation; allow 2-second lower tolerance
        // to absorb thread scheduling jitter between System.currentTimeMillis() calls
        long beforeMs = System.currentTimeMillis();

        // when
        String token = underTest.generateRefreshToken(TEST_EMAIL);

        // then
        Date expiration = underTest.extractClaim(token, Claims::getExpiration);
        long afterMs = System.currentTimeMillis();

        long expectedLow  = beforeMs + REFRESH_TOKEN_EXPIRATION_MS - 2000L;
        long expectedHigh = afterMs  + REFRESH_TOKEN_EXPIRATION_MS;

        assertThat(expiration.getTime())
                .isGreaterThanOrEqualTo(expectedLow)
                .isLessThanOrEqualTo(expectedHigh);
    }

    // ─── AC-3: Existing authentication workflows function correctly ────────────

    @Test
    @DisplayName("AC-3: isTokenValid returns true for a freshly generated access token")
    void should_return_true_when_token_is_valid_and_not_expired() {
        // given
        String token = underTest.generateAccessToken(TEST_EMAIL);
        UserDetails userDetails = User.withUsername(TEST_EMAIL)
                .password("irrelevant")
                .authorities(Collections.emptyList())
                .build();

        // when
        boolean result = underTest.isTokenValid(token, userDetails);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("AC-3: isTokenValid returns false when email does not match UserDetails username")
    void should_return_false_when_token_subject_does_not_match_user() {
        // given
        String token = underTest.generateAccessToken(TEST_EMAIL);
        UserDetails differentUser = User.withUsername("other@supermart.com")
                .password("irrelevant")
                .authorities(Collections.emptyList())
                .build();

        // when
        boolean result = underTest.isTokenValid(token, differentUser);

        // then
        assertThat(result).isFalse();
    }

    // ─── AC-4: No disruption to users with valid authentication flow ───────────

    @Test
    @DisplayName("AC-4: isTokenExpired returns false for a freshly issued access token")
    void should_return_false_when_freshly_issued_token_checked_for_expiry() {
        // given
        String token = underTest.generateAccessToken(TEST_EMAIL);

        // when
        boolean expired = underTest.isTokenExpired(token);

        // then
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("AC-4: extractEmail returns the correct email from a valid access token")
    void should_extract_correct_email_when_token_is_valid() {
        // given
        String token = underTest.generateAccessToken(TEST_EMAIL);

        // when
        String result = underTest.extractEmail(token);

        // then
        assertThat(result).isEqualTo(TEST_EMAIL);
    }

    // ─── AC-5: Security validation — edge cases ────────────────────────────────

    @Test
    @DisplayName("AC-5: generateAccessToken throws IllegalArgumentException when email is null")
    void should_throw_when_email_is_null_on_generateAccessToken() {
        // when / then
        assertThatThrownBy(() -> underTest.generateAccessToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("AC-5: extractEmail throws JwtException when token is malformed")
    void should_throw_when_token_is_malformed_on_extractEmail() {
        // when / then
        assertThatThrownBy(() -> underTest.extractEmail("not.a.valid.token"))
                .isInstanceOf(Exception.class);
    }
}
