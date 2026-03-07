package com.supermart.iot.service.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock private IotDeviceRepository deviceRepository;
    @Mock private TelemetryRepository telemetryRepository;

    @InjectMocks
    private DeviceService underTest;

    private IotDevice device;
    private Store store;
    private EquipmentUnit unit;

    @BeforeEach
    void setUp() {
        store = Store.builder()
                .storeId(1L).storeName("Store A").storeCode("SA01")
                .address("1 Main St").city("Dallas").state("TX").createdAt(LocalDateTime.now())
                .build();
        unit = EquipmentUnit.builder()
                .unitId(10L).store(store).unitType(EquipmentType.FREEZER)
                .unitName("Freezer 1").locationDesc("Aisle 2").createdAt(LocalDateTime.now())
                .build();
        device = IotDevice.builder()
                .deviceId(100L).deviceSerial("SN-100").deviceKey("k-100")
                .minTempThreshold(-20.0).maxTempThreshold(4.0)
                .status(DeviceStatus.ACTIVE).unit(unit).lastSeenAt(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // listDevices
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("listDevices returns paged summary response")
    void should_returnPagedResponse_when_devicesFound() {
        when(deviceRepository.findByStoreIdAndStatus(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(device)));
        when(telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(100L))
                .thenReturn(Optional.empty());

        PagedResponse<IotDeviceSummaryResponse> result = underTest.listDevices(null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeviceId()).isEqualTo(100L);
        assertThat(result.getContent().get(0).getIsAlert()).isFalse();
    }

    // -------------------------------------------------------------------------
    // getDeviceById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getDeviceById returns detail response when device found")
    void should_returnDetailResponse_when_deviceFound() {
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));

        IotDeviceResponse result = underTest.getDeviceById(100L);

        assertThat(result.getDeviceId()).isEqualTo(100L);
        assertThat(result.getDeviceSerial()).isEqualTo("SN-100");
        assertThat(result.getUnit().getUnitId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getDeviceById throws ResourceNotFoundException when device not found")
    void should_throwResourceNotFoundException_when_deviceNotFound() {
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getDeviceById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // -------------------------------------------------------------------------
    // getDeviceTelemetry
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getDeviceTelemetry returns paged telemetry records")
    void should_returnPagedTelemetry_when_deviceExists() {
        TelemetryRecord record = TelemetryRecord.builder()
                .telemetryId(200L).device(device).temperature(2.5)
                .recordedAt(LocalDateTime.now()).isAlert(false).build();
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(telemetryRepository.findByDeviceIdAndDateRange(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(record)));

        PagedResponse<TelemetryResponse> result = underTest.getDeviceTelemetry(100L, null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTelemetryId()).isEqualTo(200L);
        assertThat(result.getContent().get(0).getTemperature()).isEqualTo(2.5);
    }

    @Test
    @DisplayName("getDeviceTelemetry throws BadRequestException when from is after to")
    void should_throwBadRequestException_when_fromIsAfterTo() {
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.minusHours(1);

        assertThatThrownBy(() -> underTest.getDeviceTelemetry(100L, from, to, 0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("from");
    }

    // -------------------------------------------------------------------------
    // toSummaryResponse — with latest telemetry
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("toSummaryResponse includes latestTemperature and isAlert from most recent record")
    void should_includeLatestTelemetry_when_recordExists() {
        TelemetryRecord record = TelemetryRecord.builder()
                .telemetryId(1L).device(device).temperature(8.5)
                .recordedAt(LocalDateTime.now()).isAlert(true).build();
        when(telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(100L))
                .thenReturn(Optional.of(record));

        IotDeviceSummaryResponse result = underTest.toSummaryResponse(device);

        assertThat(result.getLatestTemperature()).isEqualTo(8.5);
        assertThat(result.getIsAlert()).isTrue();
    }

    // -------------------------------------------------------------------------
    // toTelemetryResponse
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("toTelemetryResponse maps all fields correctly")
    void should_mapAllFields_when_toTelemetryResponseCalled() {
        TelemetryRecord record = TelemetryRecord.builder()
                .telemetryId(300L).device(device).temperature(-18.0)
                .recordedAt(LocalDateTime.of(2026, 3, 1, 10, 0)).isAlert(false).build();

        TelemetryResponse result = underTest.toTelemetryResponse(record);

        assertThat(result.getTelemetryId()).isEqualTo(300L);
        assertThat(result.getDeviceId()).isEqualTo(100L);
        assertThat(result.getTemperature()).isEqualTo(-18.0);
        assertThat(result.getIsAlert()).isFalse();
    }
}
