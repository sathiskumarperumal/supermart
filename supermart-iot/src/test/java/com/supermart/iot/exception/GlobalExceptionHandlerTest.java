package com.supermart.iot.exception;

import com.supermart.iot.dto.response.ApiResponse;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleNotFound returns 404 with NOT_FOUND code")
    void should_return404_when_resourceNotFound() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleNotFound(new ResourceNotFoundException("Device 1 not found."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getMessage()).contains("Device 1");
    }

    @Test
    @DisplayName("handleConflict returns 409 with CONFLICT code")
    void should_return409_when_conflictException() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleConflict(new ConflictException("Duplicate entry."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getErrorCode()).isEqualTo("CONFLICT");
    }

    @Test
    @DisplayName("handleBadRequest returns 400 with VALIDATION_ERROR code")
    void should_return400_when_badRequestException() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBadRequest(new BadRequestException("Invalid input."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("handleRateLimit returns 429 with RATE_LIMITED code")
    void should_return429_when_rateLimitException() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleRateLimit(new RateLimitException("Rate limit exceeded."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RATE_LIMITED");
    }

    @Test
    @DisplayName("handleBadCredentials returns 401 with UNAUTHORIZED code")
    void should_return401_when_badCredentials() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBadCredentials(new BadCredentialsException("Bad creds"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("handleJwtException returns 401 for malformed JWT")
    void should_return401_when_jwtException() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleJwtException(new MalformedJwtException("bad jwt"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).contains("invalid or has expired");
    }

    @Test
    @DisplayName("handleAccessDenied returns 403 with FORBIDDEN code")
    void should_return403_when_accessDenied() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccessDenied(new AccessDeniedException("forbidden"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getErrorCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("handleGeneric returns 500 with INTERNAL_ERROR code")
    void should_return500_when_unexpectedException() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGeneric(new RuntimeException("Unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("handleValidation returns 400 with field errors map")
    void should_return400WithFieldErrors_when_validationFails() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errors");
        assertThat(response.getBody().get("errorCode")).isEqualTo("VALIDATION_ERROR");
    }
}
