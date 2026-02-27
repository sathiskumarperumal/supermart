package com.supermart.iot.controller;

import com.supermart.iot.dto.response.*;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.service.impl.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    void getSummary_returnsOkWithSummaryResponse() {
        DashboardSummaryResponse summary = DashboardSummaryResponse.builder()
                .totalStores(5L).activeDevices(12L).faultyDevices(3L)
                .openIncidents(2L).alertsLastHour(1L).asOf(LocalDateTime.now()).build();
        when(dashboardService.getSummary()).thenReturn(summary);

        ResponseEntity<ApiResponse<DashboardSummaryResponse>> response = dashboardController.getSummary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getTotalStores()).isEqualTo(5L);
        assertThat(response.getBody().getData().getActiveDevices()).isEqualTo(12L);
    }

    @Test
    void getAlerts_returnsOkWithPagedAlerts() {
        IotDeviceSummaryResponse alertDevice = IotDeviceSummaryResponse.builder()
                .deviceId(10L).deviceSerial("DEV-001").status(DeviceStatus.FAULT)
                .storeName("Store A").unitName("Unit A").isAlert(true).build();
        PagedResponse<IotDeviceSummaryResponse> paged = PagedResponse.<IotDeviceSummaryResponse>builder()
                .content(Collections.singletonList(alertDevice)).page(0).size(20)
                .totalElements(1).totalPages(1).build();
        when(dashboardService.getAlerts(anyInt(), anyInt())).thenReturn(paged);

        ResponseEntity<ApiResponse<PagedResponse<IotDeviceSummaryResponse>>> response =
                dashboardController.getAlerts(0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
    }
}
