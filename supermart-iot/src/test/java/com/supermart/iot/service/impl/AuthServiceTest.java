package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.LoginRequest;
import com.supermart.iot.dto.request.RefreshRequest;
import com.supermart.iot.dto.response.LoginResponse;
import com.supermart.iot.security.JwtService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
 * Covers SCRUM-9 AC-2: LoginResponse.expiresIn must equal 1800 (30 minutes).
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

    // -------------------------------------------------------------------------
    // SCRUM-9 AC-2: expiresIn must be 1800 seconds
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SCRUM-9 AC-2: login returns expiresIn=1800 after JWT expiry reduction to 30 min")
    void should_returnExpiresIn1800_when_userLogsIn() {
        // given
        LoginRequest request = LoginRequest.builder()
                .email("manager@test.com")
                .password("S3cur3P@ss!")
                .build();
        UserDetails userDetails = new User("manager@test.com", "S3cur3P@ss!", Collections.emptyList());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userDetailsService.loadUserByUsername("manager@test.com")).thenReturn(userDetails);
        when(jwtService.generateAccessToken("manager@test.com")).thenReturn("access-token-abc");
        when(jwtService.generateRefreshToken("manager@test.com")).thenReturn("refresh-token-xyz");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(1800000L); // SCRUM-9: 30 min

        // when
        LoginResponse result = underTest.login(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token-abc");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token-xyz");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(1800L); // AC-2: must be 1800, not 3600
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("SCRUM-9 AC-2: refresh returns expiresIn=1800 for new access token")
    void should_returnExpiresIn1800_when_tokenRefreshed() {
        // given
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();
        UserDetails userDetails = new User("manager@test.com", "", Collections.emptyList());

        when(jwtService.extractEmail("valid-refresh-token")).thenReturn("manager@test.com");
        when(userDetailsService.loadUserByUsername("manager@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-refresh-token", userDetails)).thenReturn(true);
        when(jwtService.generateAccessToken("manager@test.com")).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken("manager@test.com")).thenReturn("new-refresh-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(1800000L); // SCRUM-9: 30 min

        // when
        LoginResponse result = underTest.refresh(request);

        // then
        assertThat(result.getExpiresIn()).isEqualTo(1800L); // AC-2
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    }

    // -------------------------------------------------------------------------
    // Existing auth paths
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("login propagates BadCredentialsException when authentication fails")
    void should_propagateBadCredentialsException_when_authenticationFails() {
        // given
        LoginRequest request = LoginRequest.builder()
                .email("manager@test.com")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // when / then
        assertThatThrownBy(() -> underTest.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("refresh throws BadCredentialsException when refresh token is expired or invalid")
    void should_throwBadCredentialsException_when_refreshTokenInvalid() {
        // given
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("expired-refresh-token")
                .build();
        UserDetails userDetails = new User("manager@test.com", "", Collections.emptyList());

        when(jwtService.extractEmail("expired-refresh-token")).thenReturn("manager@test.com");
        when(userDetailsService.loadUserByUsername("manager@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("expired-refresh-token", userDetails)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> underTest.refresh(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token is invalid or has expired");
    }
}
