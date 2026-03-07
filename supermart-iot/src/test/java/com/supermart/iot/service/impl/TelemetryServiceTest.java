package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.TelemetryIngestRequest;
import com.supermart.iot.dto.response.TelemetryResponse;
import com.supermart.iot.entity.EquipmentUnit;
import com.supermart.iot.entity.Incident;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock private TelemetryRepository telemetryRepository;
    @Mock private IotDeviceRepository deviceRepository;
    @Mock private IncidentRepository incidentRepository;

    @InjectMocks
    private TelemetryService underTest;

    private IotDevice device;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "rateLimitPerMinute", 2);

        Store store = Store.builder()
                .storeId(1L).storeName("Store A").storeCode("SA01")
                .address("1 Main St").city("Houston").state("TX").createdAt(LocalDateTime.now())
                .build();
        EquipmentUnit unit = EquipmentUnit.builder()
                .unitId(10L).store(store).unitType(EquipmentType.REFRIGERATOR)
                .unitName("Cooler 1").locationDesc("Dairy").createdAt(LocalDateTime.now())
                .build();
        device = IotDevice.builder()
                .deviceId(100L).deviceSerial("SN-001").deviceKey("key-001")
                .minTempThreshold(0.0).maxTempThreshold(8.0)
                .status(DeviceStatus.ACTIVE).unit(unit).lastSeenAt(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // ingest — normal (within thresholds)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ingest persists telemetry without alert when temperature is within threshold")
    void should_persistTelemetryWithoutAlert_when_temperatureWithinThreshold() {
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(100L).temperature(4.0).recordedAt(LocalDateTime.now()).build();
        TelemetryRecord saved = TelemetryRecord.builder()
                .telemetryId(1L).device(device).temperature(4.0)
                .recordedAt(request.getRecordedAt()).isAlert(false).build();

        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(100L), any())).thenReturn(0L);
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(saved);
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(device);

        TelemetryResponse result = underTest.ingest(request);

        assertThat(result.getIsAlert()).isFalse();
        assertThat(result.getTemperature()).isEqualTo(4.0);
        verify(incidentRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // ingest — alert: temperature exceeds max threshold
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ingest sets isAlert and auto-creates incident when temperature exceeds max threshold")
    void should_createIncident_when_temperatureExceedsMaxThreshold() {
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(100L).temperature(12.0).recordedAt(LocalDateTime.now()).build();
        TelemetryRecord saved = TelemetryRecord.builder()
                .telemetryId(2L).device(device).temperature(12.0)
                .recordedAt(request.getRecordedAt()).isAlert(true).build();

        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(100L), any())).thenReturn(0L);
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(saved);
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(device);
        when(incidentRepository.findByDevice_DeviceIdAndStatus(100L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenReturn(mock(Incident.class));

        TelemetryResponse result = underTest.ingest(request);

        assertThat(result.getIsAlert()).isTrue();
        verify(incidentRepository).save(any(Incident.class));
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.FAULT);
    }

    // -------------------------------------------------------------------------
    // ingest — alert: temperature below min threshold
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ingest sets isAlert but skips incident creation when open incident exists")
    void should_skipIncidentCreation_when_openIncidentAlreadyExists() {
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(100L).temperature(-5.0).recordedAt(LocalDateTime.now()).build();
        TelemetryRecord saved = TelemetryRecord.builder()
                .telemetryId(3L).device(device).temperature(-5.0)
                .recordedAt(request.getRecordedAt()).isAlert(true).build();
        Incident existing = Incident.builder()
                .incidentId(10L).device(device).status(IncidentStatus.OPEN).build();

        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(100L), any())).thenReturn(0L);
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(saved);
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(device);
        when(incidentRepository.findByDevice_DeviceIdAndStatus(100L, IncidentStatus.OPEN))
                .thenReturn(Optional.of(existing));

        underTest.ingest(request);

        verify(incidentRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // ingest — rate limit exceeded
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ingest throws RateLimitException when device exceeds rate limit")
    void should_throwRateLimitException_when_rateLimitExceeded() {
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(100L).temperature(3.0).recordedAt(LocalDateTime.now()).build();
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(100L), any())).thenReturn(2L);

        assertThatThrownBy(() -> underTest.ingest(request))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("rate limit");
    }

    // -------------------------------------------------------------------------
    // ingest — device not found
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ingest throws ResourceNotFoundException when device not found")
    void should_throwResourceNotFoundException_when_deviceNotFound() {
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(999L).temperature(5.0).recordedAt(LocalDateTime.now()).build();
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.ingest(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
