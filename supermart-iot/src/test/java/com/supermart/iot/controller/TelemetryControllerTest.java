package com.supermart.iot.controller;

import com.supermart.iot.dto.request.TelemetryIngestRequest;
import com.supermart.iot.dto.response.ApiResponse;
import com.supermart.iot.dto.response.TelemetryResponse;
import com.supermart.iot.service.impl.TelemetryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryControllerTest {

    @Mock
    private TelemetryService telemetryService;

    @InjectMocks
    private TelemetryController telemetryController;

    @Test
    void ingest_normalReading_returns201WithNonAlertMessage() {
        TelemetryIngestRequest request = new TelemetryIngestRequest();
        request.setDeviceId(10L);
        request.setTemperature(5.0);
        request.setRecordedAt(LocalDateTime.now());

        TelemetryResponse record = TelemetryResponse.builder()
                .telemetryId(1L).deviceId(10L).temperature(5.0)
                .recordedAt(request.getRecordedAt()).isAlert(false).build();
        when(telemetryService.ingest(request)).thenReturn(record);

        ResponseEntity<ApiResponse<TelemetryResponse>> response = telemetryController.ingest(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("successfully");
    }

    @Test
    void ingest_alertReading_returns201WithAlertMessage() {
        TelemetryIngestRequest request = new TelemetryIngestRequest();
        request.setDeviceId(10L);
        request.setTemperature(25.0);
        request.setRecordedAt(LocalDateTime.now());

        TelemetryResponse record = TelemetryResponse.builder()
                .telemetryId(2L).deviceId(10L).temperature(25.0)
                .recordedAt(request.getRecordedAt()).isAlert(true).build();
        when(telemetryService.ingest(request)).thenReturn(record);

        ResponseEntity<ApiResponse<TelemetryResponse>> response = telemetryController.ingest(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("incident");
    }
}
