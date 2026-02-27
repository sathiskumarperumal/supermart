package com.supermart.iot.controller;

import com.supermart.iot.dto.request.LoginRequest;
import com.supermart.iot.dto.request.RefreshRequest;
import com.supermart.iot.dto.response.ApiResponse;
import com.supermart.iot.dto.response.LoginResponse;
import com.supermart.iot.service.impl.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_returnsOkWithLoginResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("pass");

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken("access-token").refreshToken("refresh-token")
                .tokenType("Bearer").expiresIn(900L).build();
        when(authService.login(request)).thenReturn(loginResponse);

        ResponseEntity<ApiResponse<LoginResponse>> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void refresh_returnsOkWithNewTokens() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid-refresh");

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken("new-access").refreshToken("new-refresh")
                .tokenType("Bearer").expiresIn(900L).build();
        when(authService.refresh(request)).thenReturn(loginResponse);

        ResponseEntity<ApiResponse<LoginResponse>> response = authController.refresh(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("new-access");
    }
}
