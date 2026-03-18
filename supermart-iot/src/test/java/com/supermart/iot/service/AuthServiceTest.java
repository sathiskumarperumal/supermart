package com.supermart.iot.service;

import com.supermart.iot.dto.request.LoginRequest;
import com.supermart.iot.dto.request.RefreshRequest;
import com.supermart.iot.dto.response.LoginResponse;
import com.supermart.iot.security.JwtService;
import com.supermart.iot.service.impl.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService} verifying the authentication workflows
 * following the SCRUM-3 JWT expiration update to 45 minutes.
 *
 * <p>Covers login, token refresh, and error paths to confirm that existing
 * workflows are not disrupted by the expiration change (AC-3 and AC-4).</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService underTest;

    private static final String TEST_EMAIL = "user@supermart.com";
    private static final String TEST_PASSWORD = "secret";
    private static final String ACCESS_TOKEN = "access.token.value";
    private static final String REFRESH_TOKEN = "refresh.token.value";
    // 2700 seconds = 45 minutes (SCRUM-3)
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 2700000L;

    // ─── AC-3 / AC-4: Login workflow functions correctly ──────────────────────

    @Test
    @DisplayName("AC-3: login returns LoginResponse with access token when credentials are valid")
    void should_return_loginResponse_when_credentials_are_valid() {
        // given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        UserDetails userDetails = buildUserDetails(TEST_EMAIL);

        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateAccessToken(TEST_EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_EMAIL)).thenReturn(REFRESH_TOKEN);
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(ACCESS_TOKEN_EXPIRATION_MS);

        // when
        LoginResponse result = underTest.login(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("AC-4: login returns expiresIn of 2700 seconds (45 minutes) per SCRUM-3")
    void should_return_expiresIn_2700_seconds_when_login_successful() {
        // given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        UserDetails userDetails = buildUserDetails(TEST_EMAIL);

        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateAccessToken(TEST_EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_EMAIL)).thenReturn(REFRESH_TOKEN);
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(ACCESS_TOKEN_EXPIRATION_MS);

        // when
        LoginResponse result = underTest.login(request);

        // then
        assertThat(result.getExpiresIn()).isEqualTo(2700L);
    }

    @Test
    @DisplayName("AC-3: login throws AuthenticationException when credentials are invalid")
    void should_throw_when_credentials_are_invalid_on_login() {
        // given
        LoginRequest request = new LoginRequest(TEST_EMAIL, "wrong-password");
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        // when / then
        assertThatThrownBy(() -> underTest.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");
    }

    // ─── AC-3: Token refresh workflow functions correctly ─────────────────────

    @Test
    @DisplayName("AC-3: refresh returns new LoginResponse when refresh token is valid")
    void should_return_new_loginResponse_when_refresh_token_is_valid() {
        // given
        RefreshRequest request = new RefreshRequest(REFRESH_TOKEN);
        UserDetails userDetails = buildUserDetails(TEST_EMAIL);

        when(jwtService.extractEmail(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(jwtService.isTokenValid(REFRESH_TOKEN, userDetails)).thenReturn(true);
        when(jwtService.generateAccessToken(TEST_EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_EMAIL)).thenReturn(REFRESH_TOKEN);
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(ACCESS_TOKEN_EXPIRATION_MS);

        // when
        LoginResponse result = underTest.refresh(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.getExpiresIn()).isEqualTo(2700L);
    }

    @Test
    @DisplayName("AC-3: refresh throws BadCredentialsException when refresh token is expired or invalid")
    void should_throw_when_refresh_token_is_invalid() {
        // given
        RefreshRequest request = new RefreshRequest(REFRESH_TOKEN);
        UserDetails userDetails = buildUserDetails(TEST_EMAIL);

        when(jwtService.extractEmail(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        when(jwtService.isTokenValid(REFRESH_TOKEN, userDetails)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> underTest.refresh(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token is invalid or has expired");
    }

    // ─── AC-5: Security validation — edge cases ────────────────────────────────

    @Test
    @DisplayName("AC-5: login throws NullPointerException when request is null")
    void should_throw_when_login_request_is_null() {
        // when / then
        assertThatThrownBy(() -> underTest.login(null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * Builds a minimal {@link UserDetails} stub for the given username.
     *
     * @param username the username (email) to set on the test user
     * @return a non-null {@link UserDetails} instance
     */
    private UserDetails buildUserDetails(String username) {
        return User.withUsername(username)
                .password("encoded-password")
                .authorities(Collections.emptyList())
                .build();
    }
}
