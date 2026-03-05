package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.TelemetryIngestRequest;
import com.supermart.iot.dto.response.TelemetryResponse;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Incident;
import com.supermart.iot.entity.TelemetryRecord;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import com.supermart.iot.exception.RateLimitException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.TelemetryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

/**
 * Unit tests for {@link TelemetryService}.
 * Covers notification trigger integration added for SCRUM-4.
 */
@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TelemetryService underTest;

    // -------------------------------------------------------------------------
    // AC-1: Notification dispatch triggered on new threshold breach incident
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-1: dispatchIncidentNotifications called when new TEMP_EXCEEDED incident is created")
    void should_callDispatchIncidentNotifications_when_newTemperatureBreachIncidentCreated() {
        // given
        ReflectionTestUtils.setField(underTest, "rateLimitPerMinute", 60);

        IotDevice device = IotDevice.builder()
                .deviceId(1L)
                .deviceSerial("SN-001")
                .minTempThreshold(-5.0)
                .maxTempThreshold(5.0)
                .status(DeviceStatus.ACTIVE)
                .build();
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(1L)
                .temperature(10.0)  // exceeds max threshold of 5.0
                .recordedAt(LocalDateTime.now())
                .build();
        TelemetryRecord savedRecord = TelemetryRecord.builder()
                .telemetryId(1L)
                .device(device)
                .temperature(10.0)
                .recordedAt(request.getRecordedAt())
                .isAlert(true)
                .build();
        Incident savedIncident = Incident.builder()
                .incidentId(1L)
                .device(device)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .status(IncidentStatus.OPEN)
                .description("Temperature exceeded max threshold of 5.0°C. Recorded: 10.0°C")
                .createdAt(LocalDateTime.now())
                .build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(savedRecord);
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(device);
        when(incidentRepository.findByDevice_DeviceIdAndStatus(1L, IncidentStatus.OPEN)).thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);

        // when
        TelemetryResponse result = underTest.ingest(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getIsAlert()).isTrue();
        verify(incidentRepository).save(any(Incident.class));
        verify(notificationService).dispatchIncidentNotifications(any(Incident.class), eq(device));
    }

    // -------------------------------------------------------------------------
    // AC-3: No duplicate notification when open incident already exists
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-3: dispatchIncidentNotifications NOT called when open incident already exists")
    void should_notCallDispatchIncidentNotifications_when_openIncidentAlreadyExists() {
        // given
        ReflectionTestUtils.setField(underTest, "rateLimitPerMinute", 60);

        IotDevice device = IotDevice.builder()
                .deviceId(1L)
                .deviceSerial("SN-001")
                .minTempThreshold(-5.0)
                .maxTempThreshold(5.0)
                .status(DeviceStatus.ACTIVE)
                .build();
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(1L)
                .temperature(10.0)  // still in alert
                .recordedAt(LocalDateTime.now())
                .build();
        TelemetryRecord savedRecord = TelemetryRecord.builder()
                .telemetryId(2L)
                .device(device)
                .temperature(10.0)
                .recordedAt(request.getRecordedAt())
                .isAlert(true)
                .build();
        Incident existingIncident = Incident.builder()
                .incidentId(1L)
                .device(device)
                .status(IncidentStatus.OPEN)
                .build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(savedRecord);
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(device);
        when(incidentRepository.findByDevice_DeviceIdAndStatus(1L, IncidentStatus.OPEN))
                .thenReturn(Optional.of(existingIncident));

        // when
        TelemetryResponse result = underTest.ingest(request);

        // then
        assertThat(result.getIsAlert()).isTrue();
        verify(incidentRepository, never()).save(any(Incident.class));
        verify(notificationService, never()).dispatchIncidentNotifications(any(), any());
    }

    // -------------------------------------------------------------------------
    // Existing: Rate limit enforcement
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Throws RateLimitException when device exceeds per-minute submission rate")
    void should_throwRateLimitException_when_deviceExceedsRateLimit() {
        // given
        ReflectionTestUtils.setField(underTest, "rateLimitPerMinute", 5);

        IotDevice device = IotDevice.builder()
                .deviceId(1L)
                .deviceSerial("SN-001")
                .minTempThreshold(-5.0)
                .maxTempThreshold(5.0)
                .status(DeviceStatus.ACTIVE)
                .build();
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(1L)
                .temperature(2.0)
                .recordedAt(LocalDateTime.now())
                .build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(1L), any(LocalDateTime.class))).thenReturn(5L);

        // when / then
        assertThatThrownBy(() -> underTest.ingest(request))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("exceeded the telemetry submission rate limit");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when device ID is not found")
    void should_throwResourceNotFoundException_when_deviceNotFound() {
        // given
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(999L)
                .temperature(2.0)
                .recordedAt(LocalDateTime.now())
                .build();
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.ingest(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
