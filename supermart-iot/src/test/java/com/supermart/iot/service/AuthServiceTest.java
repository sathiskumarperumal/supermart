package com.supermart.iot.service;

import com.supermart.iot.dto.request.LoginRequest;
import com.supermart.iot.dto.request.RefreshRequest;
import com.supermart.iot.dto.response.LoginResponse;
import com.supermart.iot.security.JwtService;
import com.supermart.iot.service.impl.AuthService;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private UserDetails buildUserDetails(String email) {
        return User.withUsername(email).password("encoded").authorities(Collections.emptyList()).build();
    }

    @Test
    void login_returnsLoginResponseOnSuccess() {
        String email = "admin@supermart.com";
        UserDetails userDetails = buildUserDetails(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.generateAccessToken(email)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(email)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password");

        LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_propagatesAuthExceptionOnBadCredentials() {
        String email = "bad@supermart.com";
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_returnsNewTokensOnValidToken() {
        String email = "admin@supermart.com";
        String refreshToken = "valid-refresh-token";
        UserDetails userDetails = buildUserDetails(email);

        when(jwtService.extractEmail(refreshToken)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(refreshToken, userDetails)).thenReturn(true);
        when(jwtService.generateAccessToken(email)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(email)).thenReturn("new-refresh-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        LoginResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void refresh_throwsBadCredentialsOnInvalidToken() {
        String email = "admin@supermart.com";
        String refreshToken = "expired-token";
        UserDetails userDetails = buildUserDetails(email);

        when(jwtService.extractEmail(refreshToken)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(refreshToken, userDetails)).thenReturn(false);

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token is invalid");
    }
}
