package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.LoginRequest;
import com.supermart.iot.dto.request.RefreshRequest;
import com.supermart.iot.dto.response.LoginResponse;
import com.supermart.iot.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Service handling authentication operations: login and token refresh.
 *
 * <p>Delegates credential validation to {@link AuthenticationManager} and
 * JWT token lifecycle management to {@link JwtService}. Access tokens issued
 * by this service expire after 60 minutes per SCRUM-64 security requirement,
 * limiting the exposure window for compromised tokens in line with OWASP A07
 * (Identification and Authentication Failures) and A05 (Security Misconfiguration).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Authenticates a user by email and password and issues a new token pair.
     *
     * <p>Delegates credential verification to Spring Security's
     * {@link AuthenticationManager}. On success, a new access token (valid for
     * 60 minutes per SCRUM-64) and refresh token are generated and returned.</p>
     *
     * @param request the login request containing the user's email and password
     * @return a {@link LoginResponse} containing the access token, refresh token,
     *         token type, and expiration duration in seconds (3600 seconds / 60 minutes)
     * @throws org.springframework.security.core.AuthenticationException
     *         if the credentials are invalid
     */
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt: email={}", request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        UserDetails user = userDetailsService.loadUserByUsername(request.getEmail());
        log.info("Login successful: email={}", request.getEmail());
        return buildLoginResponse(user.getUsername());
    }

    /**
     * Issues a new token pair using a valid refresh token.
     *
     * <p>Extracts the subject email from the refresh token, reloads the user,
     * validates the token, and generates a fresh access/refresh token pair.
     * The new access token expires after 60 minutes per SCRUM-64.</p>
     *
     * @param request the refresh request containing the unexpired refresh token
     * @return a {@link LoginResponse} containing newly issued access and refresh tokens
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if the refresh token is expired or invalid
     */
    public LoginResponse refresh(RefreshRequest request) {
        String email = jwtService.extractEmail(request.getRefreshToken());
        log.info("Token refresh requested: email={}", email);
        UserDetails user = userDetailsService.loadUserByUsername(email);
        if (!jwtService.isTokenValid(request.getRefreshToken(), user)) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Refresh token is invalid or has expired. Please log in again.");
        }
        log.info("Token refresh successful: email={}", email);
        return buildLoginResponse(email);
    }

    /**
     * Constructs a {@link LoginResponse} for the given email by generating
     * a new access and refresh token pair.
     *
     * @param email the authenticated user's email address
     * @return a fully populated {@link LoginResponse}
     */
    private LoginResponse buildLoginResponse(String email) {
        String accessToken = jwtService.generateAccessToken(email);
        String refreshToken = jwtService.generateRefreshToken(email);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .tokenType("Bearer")
                .build();
    }
}
