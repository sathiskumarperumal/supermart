package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.LoginRequest;
import com.supermart.iot.dto.request.RefreshRequest;
import com.supermart.iot.dto.response.LoginResponse;
import com.supermart.iot.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        UserDetails user = userDetailsService.loadUserByUsername(request.getEmail());
        return buildLoginResponse(user.getUsername());
    }

    public LoginResponse refresh(RefreshRequest request) {
        String email = jwtService.extractEmail(request.getRefreshToken());
        UserDetails user = userDetailsService.loadUserByUsername(email);
        if (!jwtService.isTokenValid(request.getRefreshToken(), user)) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Refresh token is invalid or has expired. Please log in again.");
        }
        return buildLoginResponse(email);
    }

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
