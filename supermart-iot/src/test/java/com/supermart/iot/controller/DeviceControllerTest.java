package com.supermart.iot.controller;

import com.supermart.iot.dto.response.*;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.service.impl.DeviceService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private DeviceController deviceController;

    @Test
    void listDevices_returnsOkWithPagedResponse() {
        IotDeviceSummaryResponse summary = IotDeviceSummaryResponse.builder()
                .deviceId(10L).deviceSerial("DEV-001").status(DeviceStatus.ACTIVE)
                .storeName("Store A").unitName("Unit A").isAlert(false).build();
        PagedResponse<IotDeviceSummaryResponse> paged = PagedResponse.<IotDeviceSummaryResponse>builder()
                .content(Collections.singletonList(summary)).page(0).size(20)
                .totalElements(1).totalPages(1).build();
        when(deviceService.listDevices(any(), any(), anyInt(), anyInt())).thenReturn(paged);

        ResponseEntity<ApiResponse<PagedResponse<IotDeviceSummaryResponse>>> response =
                deviceController.listDevices(null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
    }

    @Test
    void getDevice_returnsOkWithDeviceResponse() {
        IotDeviceResponse deviceResponse = IotDeviceResponse.builder()
                .deviceId(10L).deviceSerial("DEV-001")
                .minTempThreshold(0.0).maxTempThreshold(10.0)
                .status(DeviceStatus.ACTIVE).build();
        when(deviceService.getDeviceById(10L)).thenReturn(deviceResponse);

        ResponseEntity<ApiResponse<IotDeviceResponse>> response = deviceController.getDevice(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getDeviceId()).isEqualTo(10L);
    }

    @Test
    void getTelemetry_returnsOkWithPagedTelemetry() {
        TelemetryResponse telemetry = TelemetryResponse.builder()
                .telemetryId(1L).deviceId(10L).temperature(5.0)
                .recordedAt(LocalDateTime.now()).isAlert(false).build();
        PagedResponse<TelemetryResponse> paged = PagedResponse.<TelemetryResponse>builder()
                .content(Collections.singletonList(telemetry)).page(0).size(20)
                .totalElements(1).totalPages(1).build();
        when(deviceService.getDeviceTelemetry(eq(10L), any(), any(), anyInt(), anyInt())).thenReturn(paged);

        ResponseEntity<ApiResponse<PagedResponse<TelemetryResponse>>> response =
                deviceController.getTelemetry(10L, null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
    }

    private <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
