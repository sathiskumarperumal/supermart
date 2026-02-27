package com.supermart.iot.controller;

import com.supermart.iot.dto.response.ApiResponse;
import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.TechnicianResponse;
import com.supermart.iot.service.impl.TechnicianService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianControllerTest {

    @Mock
    private TechnicianService technicianService;

    @InjectMocks
    private TechnicianController technicianController;

    @Test
    void listTechnicians_returnsOkWithPagedResponse() {
        TechnicianResponse tech = TechnicianResponse.builder()
                .technicianId(1L).fullName("Alice Smith").email("alice@test.com")
                .phone("5551234567").region("North").build();
        PagedResponse<TechnicianResponse> paged = PagedResponse.<TechnicianResponse>builder()
                .content(Collections.singletonList(tech)).page(0).size(20)
                .totalElements(1).totalPages(1).build();
        when(technicianService.listTechnicians(any(), any(), anyInt(), anyInt())).thenReturn(paged);

        ResponseEntity<ApiResponse<PagedResponse<TechnicianResponse>>> response =
                technicianController.listTechnicians(null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        assertThat(response.getBody().getData().getContent().get(0).getFullName()).isEqualTo("Alice Smith");
    }

    @Test
    void listTechnicians_withFilters_delegatesToService() {
        PagedResponse<TechnicianResponse> empty = PagedResponse.<TechnicianResponse>builder()
                .content(Collections.emptyList()).page(0).size(20)
                .totalElements(0).totalPages(0).build();
        when(technicianService.listTechnicians(eq("South"), eq("Bob"), anyInt(), anyInt())).thenReturn(empty);

        ResponseEntity<ApiResponse<PagedResponse<TechnicianResponse>>> response =
                technicianController.listTechnicians("South", "Bob", 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent()).isEmpty();
    }

    private <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
