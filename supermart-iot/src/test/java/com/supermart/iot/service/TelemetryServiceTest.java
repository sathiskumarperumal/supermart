package com.supermart.iot.service;

import com.supermart.iot.dto.request.TelemetryIngestRequest;
import com.supermart.iot.dto.response.TelemetryResponse;
import com.supermart.iot.entity.EquipmentUnit;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Store;
import com.supermart.iot.entity.TelemetryRecord;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.EquipmentType;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.exception.RateLimitException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.TelemetryRepository;
import com.supermart.iot.service.impl.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private TelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(telemetryService, "rateLimitPerMinute", 2);
    }

    private IotDevice buildDevice(double minTemp, double maxTemp) {
        Store store = Store.builder().storeId(1L).storeName("Store A").build();
        EquipmentUnit unit = EquipmentUnit.builder()
                .unitId(1L).store(store).unitType(EquipmentType.REFRIGERATOR).unitName("Unit A").build();
        return IotDevice.builder()
                .deviceId(10L)
                .deviceSerial("DEV-001")
                .deviceKey("key-001")
                .minTempThreshold(minTemp)
                .maxTempThreshold(maxTemp)
                .status(DeviceStatus.ACTIVE)
                .unit(unit)
                .build();
    }

    private TelemetryIngestRequest buildRequest(Long deviceId, double temperature) {
        TelemetryIngestRequest req = new TelemetryIngestRequest();
        req.setDeviceId(deviceId);
        req.setTemperature(temperature);
        req.setRecordedAt(LocalDateTime.now());
        return req;
    }

    @Test
    void ingest_normalTemperature_returnsResponseWithNoAlert() {
        IotDevice device = buildDevice(0.0, 10.0);
        TelemetryIngestRequest request = buildRequest(10L, 5.0);

        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(10L), any())).thenReturn(0L);

        TelemetryRecord saved = TelemetryRecord.builder()
                .telemetryId(1L).device(device).temperature(5.0)
                .recordedAt(request.getRecordedAt()).isAlert(false).build();
        when(telemetryRepository.save(any())).thenReturn(saved);
        when(deviceRepository.save(any())).thenReturn(device);

        TelemetryResponse response = telemetryService.ingest(request);

        assertThat(response.getIsAlert()).isFalse();
        assertThat(response.getTemperature()).isEqualTo(5.0);
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void ingest_temperatureAboveMax_createsAlertAndIncident() {
        IotDevice device = buildDevice(0.0, 10.0);
        TelemetryIngestRequest request = buildRequest(10L, 15.0);

        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(10L), any())).thenReturn(0L);

        TelemetryRecord saved = TelemetryRecord.builder()
                .telemetryId(2L).device(device).temperature(15.0)
                .recordedAt(request.getRecordedAt()).isAlert(true).build();
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(saved);
        when(deviceRepository.save(any())).thenReturn(device);
        when(incidentRepository.findByDevice_DeviceIdAndStatus(10L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());

        TelemetryResponse response = telemetryService.ingest(request);

        assertThat(response.getIsAlert()).isTrue();
        verify(incidentRepository).save(any());
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.FAULT);
    }

    @Test
    void ingest_temperatureBelowMin_createsAlertAndIncident() {
        IotDevice device = buildDevice(0.0, 10.0);
        TelemetryIngestRequest request = buildRequest(10L, -5.0);

        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(10L), any())).thenReturn(0L);

        TelemetryRecord saved = TelemetryRecord.builder()
                .telemetryId(3L).device(device).temperature(-5.0)
                .recordedAt(request.getRecordedAt()).isAlert(true).build();
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(saved);
        when(deviceRepository.save(any())).thenReturn(device);
        when(incidentRepository.findByDevice_DeviceIdAndStatus(10L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());

        TelemetryResponse response = telemetryService.ingest(request);

        assertThat(response.getIsAlert()).isTrue();
        verify(incidentRepository).save(any());
    }

    @Test
    void ingest_alertButOpenIncidentExists_doesNotCreateNewIncident() {
        IotDevice device = buildDevice(0.0, 10.0);
        TelemetryIngestRequest request = buildRequest(10L, 20.0);

        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(10L), any())).thenReturn(0L);

        TelemetryRecord saved = TelemetryRecord.builder()
                .telemetryId(4L).device(device).temperature(20.0)
                .recordedAt(request.getRecordedAt()).isAlert(true).build();
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(saved);
        when(deviceRepository.save(any())).thenReturn(device);

        com.supermart.iot.entity.Incident existingIncident = com.supermart.iot.entity.Incident.builder()
                .incidentId(99L).build();
        when(incidentRepository.findByDevice_DeviceIdAndStatus(10L, IncidentStatus.OPEN))
                .thenReturn(Optional.of(existingIncident));

        telemetryService.ingest(request);

        verify(incidentRepository, never()).save(any());
    }

    @Test
    void ingest_rateLimitExceeded_throwsRateLimitException() {
        IotDevice device = buildDevice(0.0, 10.0);
        TelemetryIngestRequest request = buildRequest(10L, 5.0);

        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(10L), any())).thenReturn(2L);

        assertThatThrownBy(() -> telemetryService.ingest(request))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("rate limit");
    }

    @Test
    void ingest_deviceNotFound_throwsResourceNotFoundException() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        TelemetryIngestRequest request = buildRequest(99L, 5.0);

        assertThatThrownBy(() -> telemetryService.ingest(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
