package com.supermart.iot.service;

import com.supermart.iot.dto.response.IotDeviceResponse;
import com.supermart.iot.dto.response.IotDeviceSummaryResponse;
import com.supermart.iot.dto.response.PagedResponse;
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
import com.supermart.iot.service.impl.DeviceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private TelemetryRepository telemetryRepository;

    @InjectMocks
    private DeviceService deviceService;

    private IotDevice buildDevice() {
        Store store = Store.builder().storeId(1L).storeName("Store A").build();
        EquipmentUnit unit = EquipmentUnit.builder()
                .unitId(1L).store(store).unitType(EquipmentType.REFRIGERATOR)
                .unitName("Unit A").locationDesc("Aisle 1").createdAt(LocalDateTime.now()).build();
        return IotDevice.builder()
                .deviceId(10L).deviceSerial("DEV-001").deviceKey("key-001")
                .minTempThreshold(0.0).maxTempThreshold(10.0)
                .status(DeviceStatus.ACTIVE).unit(unit).lastSeenAt(LocalDateTime.now()).build();
    }

    @Test
    void listDevices_returnsPagedSummaries() {
        IotDevice device = buildDevice();
        Page<IotDevice> page = new PageImpl<>(List.of(device));
        when(deviceRepository.findByStoreIdAndStatus(any(), any(), any())).thenReturn(page);
        when(telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(10L)).thenReturn(Optional.empty());

        PagedResponse<IotDeviceSummaryResponse> result = deviceService.listDevices(null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeviceId()).isEqualTo(10L);
    }

    @Test
    void getDeviceById_existingId_returnsDetailResponse() {
        IotDevice device = buildDevice();
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));

        IotDeviceResponse response = deviceService.getDeviceById(10L);

        assertThat(response.getDeviceId()).isEqualTo(10L);
        assertThat(response.getDeviceSerial()).isEqualTo("DEV-001");
        assertThat(response.getMinTempThreshold()).isEqualTo(0.0);
        assertThat(response.getMaxTempThreshold()).isEqualTo(10.0);
    }

    @Test
    void getDeviceById_notFound_throwsResourceNotFoundException() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDeviceById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getDeviceTelemetry_validDateRange_returnsPagedTelemetry() {
        IotDevice device = buildDevice();
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        TelemetryRecord record = TelemetryRecord.builder()
                .telemetryId(1L).device(device).temperature(5.0)
                .recordedAt(from.plusHours(1)).isAlert(false).build();
        Page<TelemetryRecord> page = new PageImpl<>(List.of(record));

        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(telemetryRepository.findByDeviceIdAndDateRange(eq(10L), any(), any(), any())).thenReturn(page);

        PagedResponse<TelemetryResponse> result = deviceService.getDeviceTelemetry(10L, from, to, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTemperature()).isEqualTo(5.0);
    }

    @Test
    void getDeviceTelemetry_fromAfterTo_throwsBadRequestException() {
        IotDevice device = buildDevice();
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = LocalDateTime.now().minusDays(1);

        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> deviceService.getDeviceTelemetry(10L, from, to, 0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("before");
    }

    @Test
    void getDeviceTelemetry_nullDates_doesNotThrow() {
        IotDevice device = buildDevice();
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(telemetryRepository.findByDeviceIdAndDateRange(eq(10L), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<TelemetryResponse> result = deviceService.getDeviceTelemetry(10L, null, null, 0, 10);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void toSummaryResponse_withLatestTelemetry_includesTemperature() {
        IotDevice device = buildDevice();
        TelemetryRecord latest = TelemetryRecord.builder()
                .telemetryId(1L).device(device).temperature(7.5)
                .recordedAt(LocalDateTime.now()).isAlert(false).build();
        when(telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(10L))
                .thenReturn(Optional.of(latest));

        IotDeviceSummaryResponse summary = deviceService.toSummaryResponse(device);

        assertThat(summary.getLatestTemperature()).isEqualTo(7.5);
        assertThat(summary.getIsAlert()).isFalse();
    }

    @Test
    void toSummaryResponse_withNoTelemetry_hasNullTemperature() {
        IotDevice device = buildDevice();
        when(telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(10L))
                .thenReturn(Optional.empty());

        IotDeviceSummaryResponse summary = deviceService.toSummaryResponse(device);

        assertThat(summary.getLatestTemperature()).isNull();
        assertThat(summary.getIsAlert()).isFalse();
    }

    private <T> T isNull() {
        return org.mockito.ArgumentMatchers.isNull();
    }

    private <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
