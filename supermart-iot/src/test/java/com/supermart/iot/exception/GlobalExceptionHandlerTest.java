package com.supermart.iot.exception;

import com.supermart.iot.dto.response.ApiResponse;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * Verifies correct HTTP status codes and error codes for all mapped exception types.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler underTest;

    @Test
    @DisplayName("handleNotFound returns 404 NOT_FOUND with errorCode NOT_FOUND")
    void should_return404_when_resourceNotFoundExceptionThrown() {
        // given
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleNotFound(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("Resource not found");
    }

    @Test
    @DisplayName("handleConflict returns 409 CONFLICT with errorCode CONFLICT")
    void should_return409_when_conflictExceptionThrown() {
        // given
        ConflictException ex = new ConflictException("Conflict detected");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleConflict(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("CONFLICT");
    }

    @Test
    @DisplayName("handleBadRequest returns 400 BAD_REQUEST with errorCode VALIDATION_ERROR")
    void should_return400_when_badRequestExceptionThrown() {
        // given
        BadRequestException ex = new BadRequestException("Invalid input");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleBadRequest(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input");
    }

    @Test
    @DisplayName("handleRateLimit returns 429 TOO_MANY_REQUESTS with errorCode RATE_LIMITED")
    void should_return429_when_rateLimitExceptionThrown() {
        // given
        RateLimitException ex = new RateLimitException("Rate limit exceeded");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleRateLimit(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("RATE_LIMITED");
    }

    @Test
    @DisplayName("handleBadCredentials returns 401 UNAUTHORIZED with errorCode UNAUTHORIZED")
    void should_return401_when_badCredentialsExceptionThrown() {
        // given
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleBadCredentials(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("handleJwtException returns 401 UNAUTHORIZED when JWT is invalid")
    void should_return401_when_jwtExceptionThrown() {
        // given
        JwtException ex = new MalformedJwtException("JWT malformed");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleJwtException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("handleAccessDenied returns 403 FORBIDDEN with errorCode FORBIDDEN")
    void should_return403_when_accessDeniedExceptionThrown() {
        // given
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleAccessDenied(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("handleGeneric returns 500 INTERNAL_SERVER_ERROR with errorCode INTERNAL_ERROR")
    void should_return500_when_unexpectedExceptionThrown() {
        // given
        Exception ex = new RuntimeException("Unexpected error");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleGeneric(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("handleValidation returns 400 with field error details in response body")
    void should_return400WithFieldErrors_when_methodArgumentNotValidExceptionThrown() {
        // given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("request", "emailAddress", "emailAddress is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // when
        ResponseEntity<Map<String, Object>> response = underTest.handleValidation(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("errorCode")).isEqualTo("VALIDATION_ERROR");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertThat(errors).containsKey("emailAddress");
    }
}
