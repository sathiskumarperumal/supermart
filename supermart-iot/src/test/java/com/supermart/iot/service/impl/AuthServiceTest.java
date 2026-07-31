package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.LoginRequest;
import com.supermart.iot.dto.request.RefreshRequest;
import com.supermart.iot.dto.response.LoginResponse;
import com.supermart.iot.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService} covering SCRUM-67 JWT expiration change.
 *
 * <p>Verifies that login and refresh workflows continue to function correctly
 * (AC-3, AC-4) and that the {@code expiresIn} field on {@link LoginResponse}
 * reflects the reduced 30-minute (1800 second) access token lifetime (AC-1, AC-2)
 * introduced by SCRUM-67 (previously 45 minutes / 2700 seconds under SCRUM-3).</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TEST_EMAIL = "admin@supermart.com";
    private static final String TEST_PASSWORD = "S3cur3P@ss!";
    private static final String ACCESS_TOKEN = "access.token.value";
    private static final String REFRESH_TOKEN = "refresh.token.value";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 1800000L; // 30 minutes (SCRUM-67)

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService underTest;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = User.withUsername(TEST_EMAIL)
                .password("encoded-password")
                .authorities(Collections.emptyList())
                .build();
    }

    // ─── AC-3: Existing authentication workflows function correctly ────────────

    @Test
    @DisplayName("AC-3: login returns a LoginResponse with tokens when credentials are valid")
    void should_return_loginResponse_when_login_credentials_are_valid() {
        // given
        LoginRequest request = LoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateAccessToken(TEST_EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_EMAIL)).thenReturn(REFRESH_TOKEN);
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(ACCESS_TOKEN_EXPIRATION_MS);

        // when
        LoginResponse response = underTest.login(request);

        // then
        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(authenticationManager, times(1)).authenticate(any());
    }

    // ─── AC-1 / AC-2: expiresIn reflects the reduced 30-minute (1800s) lifetime ─

    @Test
    @DisplayName("AC-1/AC-2: login response expiresIn equals 1800 seconds (30 minutes) reflecting the reduced JWT expiry")
    void should_set_expiresIn_to_1800_seconds_when_login_succeeds() {
        // given
        LoginRequest request = LoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateAccessToken(TEST_EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_EMAIL)).thenReturn(REFRESH_TOKEN);
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(ACCESS_TOKEN_EXPIRATION_MS);

        // when
        LoginResponse response = underTest.login(request);

        // then
        assertThat(response.getExpiresIn()).isEqualTo(1800L);
    }

    // ─── AC-3 / AC-4: refresh workflow continues to function correctly, no disruption ─

    @Test
    @DisplayName("AC-3: refresh returns a new LoginResponse when the refresh token is valid")
    void should_return_new_loginResponse_when_refresh_token_is_valid() {
        // given
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken(REFRESH_TOKEN)
                .build();
        when(jwtService.extractEmail(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(jwtService.isTokenValid(REFRESH_TOKEN, userDetails)).thenReturn(true);
        when(jwtService.generateAccessToken(TEST_EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_EMAIL)).thenReturn(REFRESH_TOKEN);
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(ACCESS_TOKEN_EXPIRATION_MS);

        // when
        LoginResponse response = underTest.refresh(request);

        // then
        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getExpiresIn()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("AC-4: refresh throws BadCredentialsException when the refresh token is invalid or expired")
    void should_throw_badCredentialsException_when_refresh_token_is_invalid() {
        // given
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken(REFRESH_TOKEN)
                .build();
        when(jwtService.extractEmail(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(jwtService.isTokenValid(REFRESH_TOKEN, userDetails)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> underTest.refresh(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token is invalid or has expired");
    }

    // ─── AC-5: Security validation — edge case ─────────────────────────────────

    @Test
    @DisplayName("AC-5: login propagates AuthenticationException when credentials are invalid")
    void should_propagate_authenticationException_when_credentials_are_invalid() {
        // given
        LoginRequest request = LoginRequest.builder()
                .email(TEST_EMAIL)
                .password("wrong-password")
                .build();
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        // when / then
        assertThatThrownBy(() -> underTest.login(request))
                .isInstanceOf(BadCredentialsException.class);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }
}
