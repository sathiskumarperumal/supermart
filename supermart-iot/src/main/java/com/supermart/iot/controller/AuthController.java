package com.supermart.iot.controller;

import com.supermart.iot.dto.request.LoginRequest;
import com.supermart.iot.dto.request.RefreshRequest;
import com.supermart.iot.dto.response.ApiResponse;
import com.supermart.iot.dto.response.LoginResponse;
import com.supermart.iot.service.impl.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and token management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
        summary = "Authenticate user and obtain JWT tokens",
        description = "Returns a Bearer access token (expires in 1800s / 30 min) and a refresh token. " +
                      "SCRUM-9: access token lifetime reduced from 60 min to 30 min per OWASP API Security recommendations."
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token using a valid refresh token",
        description = "Issues a new access token (expires in 1800s / 30 min) using a valid refresh token. " +
                      "The refresh token itself is also rotated. " +
                      "SCRUM-9: access token lifetime is 30 min; refresh token lifetime is unchanged."
    )
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }
}
