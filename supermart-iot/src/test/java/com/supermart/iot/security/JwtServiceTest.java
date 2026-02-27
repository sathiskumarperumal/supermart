package com.supermart.iot.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long ACCESS_EXPIRY_MS = 900000L;
    private static final long REFRESH_EXPIRY_MS = 86400000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", ACCESS_EXPIRY_MS);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", REFRESH_EXPIRY_MS);
    }

    @Test
    void generateAccessToken_returnsNonNullToken() {
        String token = jwtService.generateAccessToken("user@test.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void generateRefreshToken_returnsNonNullToken() {
        String token = jwtService.generateRefreshToken("user@test.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String email = "user@test.com";
        String token = jwtService.generateAccessToken(email);
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        String email = "admin@test.com";
        String token = jwtService.generateAccessToken(email);
        UserDetails userDetails = User.withUsername(email)
                .password("pass").authorities(Collections.emptyList()).build();
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForWrongUser() {
        String token = jwtService.generateAccessToken("user@test.com");
        UserDetails otherUser = User.withUsername("other@test.com")
                .password("pass").authorities(Collections.emptyList()).build();
        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenExpired_returnsFalseForFreshToken() {
        String token = jwtService.generateAccessToken("user@test.com");
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenExpired_throwsExpiredJwtExceptionForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", -1000L);
        String token = jwtService.generateAccessToken("user@test.com");
        // JJWT 0.12.x throws ExpiredJwtException during parsing rather than returning true
        assertThatThrownBy(() -> jwtService.isTokenExpired(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void getAccessTokenExpirationMs_returnsConfiguredValue() {
        assertThat(jwtService.getAccessTokenExpirationMs()).isEqualTo(ACCESS_EXPIRY_MS);
    }

    @Test
    void extractEmail_throwsForInvalidToken() {
        assertThatThrownBy(() -> jwtService.extractEmail("not.a.valid.token"))
                .isInstanceOf(Exception.class);
    }
}
