package com.supermart.iot.dto;

import com.supermart.iot.dto.response.ApiResponse;
import com.supermart.iot.dto.response.PagedResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApiResponse} and {@link PagedResponse} factory methods.
 *
 * <p>Verifies that the static factory methods produce correctly populated
 * response objects, covering all success and error code paths.</p>
 */
class ApiResponseTest {

    // ─── ApiResponse.ok(data) ─────────────────────────────────────────────────

    @Test
    @DisplayName("ApiResponse.ok(data) sets success=true and message=OK")
    void should_set_success_true_and_message_ok_when_ok_called_with_data_only() {
        // when
        ApiResponse<String> response = ApiResponse.ok("test-data");

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test-data");
        assertThat(response.getMessage()).isEqualTo("OK");
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    // ─── ApiResponse.ok(data, message) ───────────────────────────────────────

    @Test
    @DisplayName("ApiResponse.ok(data, message) sets custom message and success=true")
    void should_set_custom_message_when_ok_called_with_data_and_message() {
        // when
        ApiResponse<String> response = ApiResponse.ok("payload", "Incident created successfully.");

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("payload");
        assertThat(response.getMessage()).isEqualTo("Incident created successfully.");
        assertThat(response.getTimestamp()).isNotNull();
    }

    // ─── ApiResponse.error ────────────────────────────────────────────────────

    @Test
    @DisplayName("ApiResponse.error sets success=false with error code and message")
    void should_set_success_false_with_error_code_when_error_called() {
        // when
        ApiResponse<Void> response = ApiResponse.error("NOT_FOUND", "Resource not found.");

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getMessage()).isEqualTo("Resource not found.");
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    // ─── PagedResponse.of ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PagedResponse.of maps Spring Page fields correctly")
    void should_map_page_fields_when_paged_response_of_called() {
        // given
        List<String> items = List.of("item1", "item2", "item3");
        Page<String> page = new PageImpl<>(items, PageRequest.of(1, 3), 9);

        // when
        PagedResponse<String> response = PagedResponse.of(page);

        // then
        assertThat(response.getContent()).containsExactlyInAnyOrder("item1", "item2", "item3");
        assertThat(response.getTotalElements()).isEqualTo(9L);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("PagedResponse.of handles empty page correctly")
    void should_return_empty_content_when_page_has_no_elements() {
        // given
        Page<String> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        // when
        PagedResponse<String> response = PagedResponse.of(emptyPage);

        // then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
    }
}
