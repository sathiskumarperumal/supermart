package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.TelemetryIngestRequest;
import com.supermart.iot.dto.response.TelemetryResponse;
import com.supermart.iot.entity.EquipmentUnit;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Store;
import com.supermart.iot.entity.TelemetryRecord;
import com.supermart.iot.enums.DeviceStatus;
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

/**
 * Unit tests for {@link TelemetryService}.
 *
 * <p>Covers telemetry ingestion, threshold evaluation, auto-incident creation,
 * rate-limit enforcement, and error paths.</p>
 */
@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private TelemetryService underTest;

    private IotDevice device;
    private TelemetryRecord savedRecord;

    @BeforeEach
    void setUp() {
        // Inject rate limit via ReflectionTestUtils (value normally injected by @Value)
        ReflectionTestUtils.setField(underTest, "rateLimitPerMinute", 10);

        Store store = Store.builder().storeId(1001L).storeName("Supermart Dallas").build();
        EquipmentUnit unit = EquipmentUnit.builder().unitId(501L).store(store).unitName("Freezer-1").build();

        device = IotDevice.builder()
                .deviceId(9001L)
                .unit(unit)
                .deviceSerial("DEV-9001")
                .deviceKey("key-9001")
                .status(DeviceStatus.ACTIVE)
                .minTempThreshold(-25.0)
                .maxTempThreshold(-15.0)
                .lastSeenAt(LocalDateTime.now())
                .build();

        savedRecord = TelemetryRecord.builder()
                .telemetryId(78234441L)
                .device(device)
                .temperature(-18.0)
                .recordedAt(LocalDateTime.now())
                .isAlert(false)
                .build();
    }

    // ─── ingest — normal path ─────────────────────────────────────────────────

    @Test
    @DisplayName("ingest returns TelemetryResponse when temperature is within threshold")
    void should_return_telemetry_response_when_temperature_within_threshold() {
        // given
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(9001L)
                .temperature(-18.0)
                .recordedAt(LocalDateTime.now())
                .build();

        when(deviceRepository.findById(9001L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(9001L), any())).thenReturn(3L);
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(savedRecord);
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(device);

        // when
        TelemetryResponse result = underTest.ingest(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo(9001L);
        assertThat(result.getTemperature()).isEqualTo(-18.0);
        assertThat(result.getIsAlert()).isFalse();
        verify(incidentRepository, never()).save(any());
    }

    @Test
    @DisplayName("ingest sets device status to FAULT and creates incident when temperature exceeds max threshold")
    void should_set_fault_status_and_create_incident_when_temperature_exceeds_max_threshold() {
        // given
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(9001L)
                .temperature(-10.0) // above max of -15.0 => alert
                .recordedAt(LocalDateTime.now())
                .build();

        TelemetryRecord alertRecord = TelemetryRecord.builder()
                .telemetryId(78234442L)
                .device(device)
                .temperature(-10.0)
                .recordedAt(LocalDateTime.now())
                .isAlert(true)
                .build();

        when(deviceRepository.findById(9001L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(9001L), any())).thenReturn(1L);
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(alertRecord);
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(device);
        when(incidentRepository.findByDevice_DeviceIdAndStatus(9001L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any())).thenReturn(null);

        // when
        TelemetryResponse result = underTest.ingest(request);

        // then
        assertThat(result.getIsAlert()).isTrue();
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.FAULT);
        verify(incidentRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("ingest does not create duplicate incident when open incident already exists")
    void should_not_create_incident_when_open_incident_already_exists_for_device() {
        // given
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(9001L)
                .temperature(-10.0) // alert
                .recordedAt(LocalDateTime.now())
                .build();

        TelemetryRecord alertRecord = TelemetryRecord.builder()
                .telemetryId(78234443L)
                .device(device)
                .temperature(-10.0)
                .recordedAt(LocalDateTime.now())
                .isAlert(true)
                .build();

        when(deviceRepository.findById(9001L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(9001L), any())).thenReturn(1L);
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(alertRecord);
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(device);
        when(incidentRepository.findByDevice_DeviceIdAndStatus(9001L, IncidentStatus.OPEN))
                .thenReturn(Optional.of(com.supermart.iot.entity.Incident.builder()
                        .incidentId(3301L)
                        .status(IncidentStatus.OPEN)
                        .build()));

        // when
        TelemetryResponse result = underTest.ingest(request);

        // then
        assertThat(result.getIsAlert()).isTrue();
        verify(incidentRepository, never()).save(any());
    }

    @Test
    @DisplayName("ingest creates incident with below-min description when temperature is below min threshold")
    void should_create_incident_with_below_min_description_when_temperature_below_min_threshold() {
        // given
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(9001L)
                .temperature(-30.0) // below min of -25.0 => alert
                .recordedAt(LocalDateTime.now())
                .build();

        TelemetryRecord alertRecord = TelemetryRecord.builder()
                .telemetryId(78234444L)
                .device(device)
                .temperature(-30.0)
                .recordedAt(LocalDateTime.now())
                .isAlert(true)
                .build();

        when(deviceRepository.findById(9001L)).thenReturn(Optional.of(device));
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(9001L), any())).thenReturn(0L);
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(alertRecord);
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(device);
        when(incidentRepository.findByDevice_DeviceIdAndStatus(9001L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any())).thenReturn(null);

        // when
        TelemetryResponse result = underTest.ingest(request);

        // then
        assertThat(result.getIsAlert()).isTrue();
        verify(incidentRepository, times(1)).save(argThat(incident ->
                ((com.supermart.iot.entity.Incident) incident).getDescription().contains("below min threshold")));
    }

    // ─── ingest — error paths ─────────────────────────────────────────────────

    @Test
    @DisplayName("ingest throws ResourceNotFoundException when device does not exist")
    void should_throw_resource_not_found_when_device_not_found_on_ingest() {
        // given
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(9999L)
                .temperature(-18.0)
                .recordedAt(LocalDateTime.now())
                .build();

        when(deviceRepository.findById(9999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.ingest(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");
    }

    @Test
    @DisplayName("ingest throws RateLimitException when rate limit is exceeded for device")
    void should_throw_rate_limit_exception_when_rate_limit_exceeded() {
        // given
        TelemetryIngestRequest request = TelemetryIngestRequest.builder()
                .deviceId(9001L)
                .temperature(-18.0)
                .recordedAt(LocalDateTime.now())
                .build();

        when(deviceRepository.findById(9001L)).thenReturn(Optional.of(device));
        // Return count >= rateLimitPerMinute (10)
        when(telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(eq(9001L), any())).thenReturn(10L);

        // when / then
        assertThatThrownBy(() -> underTest.ingest(request))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("rate limit");
    }
}
