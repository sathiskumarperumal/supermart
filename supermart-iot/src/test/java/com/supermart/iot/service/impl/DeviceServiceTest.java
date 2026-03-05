package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.IotDeviceResponse;
import com.supermart.iot.dto.response.IotDeviceSummaryResponse;
import com.supermart.iot.dto.response.TelemetryResponse;
import com.supermart.iot.entity.EquipmentUnit;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Store;
import com.supermart.iot.entity.TelemetryRecord;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.EquipmentType;
import com.supermart.iot.exception.BadRequestException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.TelemetryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeviceService}.
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private TelemetryRepository telemetryRepository;

    @InjectMocks
    private DeviceService underTest;

    private IotDevice buildDevice(Long deviceId) {
        Store store = Store.builder().storeId(1L).storeName("Store A").build();
        EquipmentUnit unit = EquipmentUnit.builder()
                .unitId(1L)
                .unitName("Freezer 1")
                .unitType(EquipmentType.FREEZER)
                .locationDesc("Aisle 3")
                .store(store)
                .createdAt(LocalDateTime.now())
                .build();
        return IotDevice.builder()
                .deviceId(deviceId)
                .deviceSerial("SN-00" + deviceId)
                .unit(unit)
                .minTempThreshold(-5.0)
                .maxTempThreshold(5.0)
                .status(DeviceStatus.ACTIVE)
                .lastSeenAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getDeviceById returns device response when device exists")
    void should_returnDeviceResponse_when_deviceFoundById() {
        // given
        Long deviceId = 1L;
        IotDevice device = buildDevice(deviceId);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        // when
        IotDeviceResponse result = underTest.getDeviceById(deviceId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo(deviceId);
        assertThat(result.getDeviceSerial()).isEqualTo("SN-001");
    }

    @Test
    @DisplayName("getDeviceById throws ResourceNotFoundException when device not found")
    void should_throwResourceNotFoundException_when_deviceNotFoundById() {
        // given
        Long deviceId = 999L;
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.getDeviceById(deviceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("toSummaryResponse returns device summary with latest telemetry")
    void should_returnSummaryWithLatestTemperature_when_telemetryExists() {
        // given
        Long deviceId = 1L;
        IotDevice device = buildDevice(deviceId);
        TelemetryRecord latestRecord = TelemetryRecord.builder()
                .telemetryId(10L)
                .device(device)
                .temperature(3.5)
                .isAlert(false)
                .recordedAt(LocalDateTime.now())
                .build();

        when(telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(deviceId))
                .thenReturn(Optional.of(latestRecord));

        // when
        IotDeviceSummaryResponse result = underTest.toSummaryResponse(device);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo(deviceId);
        assertThat(result.getLatestTemperature()).isEqualTo(3.5);
        assertThat(result.getIsAlert()).isFalse();
    }

    @Test
    @DisplayName("toSummaryResponse returns null temperature when no telemetry exists")
    void should_returnNullTemperature_when_noTelemetryExists() {
        // given
        Long deviceId = 1L;
        IotDevice device = buildDevice(deviceId);

        when(telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(deviceId))
                .thenReturn(Optional.empty());

        // when
        IotDeviceSummaryResponse result = underTest.toSummaryResponse(device);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLatestTemperature()).isNull();
        assertThat(result.getIsAlert()).isFalse();
    }

    @Test
    @DisplayName("getDeviceTelemetry throws BadRequestException when from is after to")
    void should_throwBadRequestException_when_fromDateIsAfterToDate() {
        // given
        Long deviceId = 1L;
        IotDevice device = buildDevice(deviceId);
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.minusDays(1);

        // when / then
        assertThatThrownBy(() -> underTest.getDeviceTelemetry(deviceId, from, to, 0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("'from' date must be before 'to' date");
    }

    @Test
    @DisplayName("getDeviceTelemetry returns paged telemetry records when valid range given")
    void should_returnPagedTelemetry_when_validDateRangeProvided() {
        // given
        Long deviceId = 1L;
        IotDevice device = buildDevice(deviceId);
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        TelemetryRecord record = TelemetryRecord.builder()
                .telemetryId(1L)
                .device(device)
                .temperature(2.5)
                .recordedAt(from.plusHours(1))
                .isAlert(false)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(telemetryRepository.findByDeviceIdAndDateRange(eq(deviceId), eq(from), eq(to), any()))
                .thenReturn(new PageImpl<>(List.of(record)));

        // when
        var result = underTest.getDeviceTelemetry(deviceId, from, to, 0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        TelemetryResponse first = result.getContent().get(0);
        assertThat(first.getTemperature()).isEqualTo(2.5);
        assertThat(first.getIsAlert()).isFalse();
    }

    @Test
    @DisplayName("listDevices returns paged device summaries")
    void should_returnPagedDeviceSummaries_when_listDevicesCalled() {
        // given
        Long deviceId = 1L;
        IotDevice device = buildDevice(deviceId);
        TelemetryRecord record = TelemetryRecord.builder()
                .telemetryId(1L).device(device).temperature(1.5).isAlert(false).recordedAt(LocalDateTime.now()).build();

        when(deviceRepository.findByStoreIdAndStatus(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(device)));
        when(telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(deviceId))
                .thenReturn(Optional.of(record));

        // when
        var result = underTest.listDevices(null, null, 0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        IotDeviceSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.getDeviceId()).isEqualTo(deviceId);
        assertThat(summary.getLatestTemperature()).isEqualTo(1.5);
    }
}
