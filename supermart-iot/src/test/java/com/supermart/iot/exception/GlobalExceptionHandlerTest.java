package com.supermart.iot.exception;

import com.supermart.iot.dto.response.ApiResponse;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
 *
 * <p>Verifies that each exception type returns the correct HTTP status,
 * error code, and message payload.</p>
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler underTest;

    // ─── ResourceNotFoundException ────────────────────────────────────────────

    @Test
    @DisplayName("handleNotFound returns 404 with NOT_FOUND error code")
    void should_return_404_when_resource_not_found_exception_thrown() {
        // given
        ResourceNotFoundException ex = new ResourceNotFoundException("Device with id 9999 not found.");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleNotFound(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getMessage()).contains("9999");
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // ─── ConflictException ────────────────────────────────────────────────────

    @Test
    @DisplayName("handleConflict returns 409 with CONFLICT error code")
    void should_return_409_when_conflict_exception_thrown() {
        // given
        ConflictException ex = new ConflictException("Open incident already exists.");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleConflict(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getErrorCode()).isEqualTo("CONFLICT");
        assertThat(response.getBody().getMessage()).contains("Open incident");
    }

    // ─── BadRequestException ──────────────────────────────────────────────────

    @Test
    @DisplayName("handleBadRequest returns 400 with VALIDATION_ERROR error code")
    void should_return_400_when_bad_request_exception_thrown() {
        // given
        BadRequestException ex = new BadRequestException("'from' date must be before 'to' date.");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleBadRequest(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getMessage()).contains("from");
    }

    // ─── RateLimitException ───────────────────────────────────────────────────

    @Test
    @DisplayName("handleRateLimit returns 429 with RATE_LIMITED error code")
    void should_return_429_when_rate_limit_exception_thrown() {
        // given
        RateLimitException ex = new RateLimitException("Too many requests.");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleRateLimit(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RATE_LIMITED");
    }

    // ─── BadCredentialsException ──────────────────────────────────────────────

    @Test
    @DisplayName("handleBadCredentials returns 401 with UNAUTHORIZED error code")
    void should_return_401_when_bad_credentials_exception_thrown() {
        // given
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleBadCredentials(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().getMessage()).contains("Invalid email or password");
    }

    // ─── JwtException ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("handleJwtException returns 401 with UNAUTHORIZED error code for malformed token")
    void should_return_401_when_jwt_exception_thrown() {
        // given
        MalformedJwtException ex = new MalformedJwtException("Malformed JWT token.");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleJwtException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().getMessage()).contains("invalid or has expired");
    }

    // ─── AccessDeniedException ────────────────────────────────────────────────

    @Test
    @DisplayName("handleAccessDenied returns 403 with FORBIDDEN error code")
    void should_return_403_when_access_denied_exception_thrown() {
        // given
        AccessDeniedException ex = new AccessDeniedException("Access denied.");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleAccessDenied(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getErrorCode()).isEqualTo("FORBIDDEN");
        assertThat(response.getBody().getMessage()).contains("permission");
    }

    // ─── MethodArgumentNotValidException ──────────────────────────────────────

    @Test
    @DisplayName("handleValidation returns 400 with VALIDATION_ERROR and field errors")
    void should_return_400_with_field_errors_when_method_argument_not_valid_exception_thrown() {
        // given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("createIncidentRequest", "deviceId", "deviceId is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // when
        ResponseEntity<Map<String, Object>> response = underTest.handleValidation(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("errorCode")).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().get("success")).isEqualTo(false);
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("errors");
        assertThat(fieldErrors).containsEntry("deviceId", "deviceId is required");
    }

    // ─── Generic Exception ────────────────────────────────────────────────────

    @Test
    @DisplayName("handleGeneric returns 500 with INTERNAL_ERROR error code for unexpected exceptions")
    void should_return_500_when_unexpected_exception_thrown() {
        // given
        RuntimeException ex = new RuntimeException("Unexpected error.");

        // when
        ResponseEntity<ApiResponse<Void>> response = underTest.handleGeneric(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).contains("unexpected error");
    }
}
