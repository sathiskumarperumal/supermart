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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
 *
 * <p>Covers device listing, device retrieval, telemetry pagination,
 * and error paths including date-range validation.</p>
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private TelemetryRepository telemetryRepository;

    @InjectMocks
    private DeviceService underTest;

    private IotDevice device;
    private Store store;
    private EquipmentUnit unit;
    private TelemetryRecord telemetryRecord;

    @BeforeEach
    void setUp() {
        store = Store.builder()
                .storeId(1001L)
                .storeName("Supermart Dallas")
                .storeCode("TX-DAL-042")
                .build();

        unit = EquipmentUnit.builder()
                .unitId(501L)
                .store(store)
                .unitType(EquipmentType.FREEZER)
                .unitName("Freezer-Aisle-3")
                .locationDesc("Aisle 3")
                .createdAt(LocalDateTime.now())
                .build();

        device = IotDevice.builder()
                .deviceId(9001L)
                .unit(unit)
                .deviceSerial("DEV-2024-TX-09001")
                .deviceKey("key-dev-9001")
                .status(DeviceStatus.ACTIVE)
                .minTempThreshold(-25.0)
                .maxTempThreshold(-15.0)
                .lastSeenAt(LocalDateTime.now())
                .build();

        telemetryRecord = TelemetryRecord.builder()
                .telemetryId(78234441L)
                .device(device)
                .temperature(-14.8)
                .recordedAt(LocalDateTime.now())
                .isAlert(false)
                .build();
    }

    // ─── listDevices ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("listDevices returns paged summary responses when devices exist")
    void should_return_paged_device_summaries_when_devices_found() {
        // given
        Page<IotDevice> page = new PageImpl<>(List.of(device));
        when(deviceRepository.findByStoreIdAndStatus(any(), any(), any(Pageable.class))).thenReturn(page);
        when(telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(9001L))
                .thenReturn(Optional.of(telemetryRecord));

        // when
        PagedResponse<IotDeviceSummaryResponse> result = underTest.listDevices(null, null, 0, 20);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeviceId()).isEqualTo(9001L);
        assertThat(result.getContent().get(0).getLatestTemperature()).isEqualTo(-14.8);
    }

    @Test
    @DisplayName("listDevices returns isAlert false when no telemetry exists")
    void should_return_is_alert_false_when_no_telemetry_record_found() {
        // given
        Page<IotDevice> page = new PageImpl<>(List.of(device));
        when(deviceRepository.findByStoreIdAndStatus(any(), any(), any(Pageable.class))).thenReturn(page);
        when(telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(9001L))
                .thenReturn(Optional.empty());

        // when
        PagedResponse<IotDeviceSummaryResponse> result = underTest.listDevices(1001L, DeviceStatus.ACTIVE, 0, 20);

        // then
        assertThat(result.getContent().get(0).getIsAlert()).isFalse();
        assertThat(result.getContent().get(0).getLatestTemperature()).isNull();
    }

    // ─── getDeviceById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDeviceById returns full device response when device found")
    void should_return_device_response_when_device_found_by_id() {
        // given
        when(deviceRepository.findById(9001L)).thenReturn(Optional.of(device));

        // when
        IotDeviceResponse result = underTest.getDeviceById(9001L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo(9001L);
        assertThat(result.getDeviceSerial()).isEqualTo("DEV-2024-TX-09001");
        assertThat(result.getMinTempThreshold()).isEqualTo(-25.0);
        assertThat(result.getMaxTempThreshold()).isEqualTo(-15.0);
    }

    @Test
    @DisplayName("getDeviceById throws ResourceNotFoundException when device not found")
    void should_throw_resource_not_found_when_device_not_found_by_id() {
        // given
        when(deviceRepository.findById(9999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.getDeviceById(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");
    }

    // ─── getDeviceTelemetry ───────────────────────────────────────────────────

    @Test
    @DisplayName("getDeviceTelemetry returns paged telemetry when valid date range provided")
    void should_return_paged_telemetry_when_valid_date_range_provided() {
        // given
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();
        Page<TelemetryRecord> page = new PageImpl<>(List.of(telemetryRecord));

        when(deviceRepository.findById(9001L)).thenReturn(Optional.of(device));
        when(telemetryRepository.findByDeviceIdAndDateRange(eq(9001L), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(page);

        // when
        PagedResponse<TelemetryResponse> result = underTest.getDeviceTelemetry(9001L, from, to, 0, 20);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTemperature()).isEqualTo(-14.8);
    }

    @Test
    @DisplayName("getDeviceTelemetry throws BadRequestException when from is after to")
    void should_throw_bad_request_when_from_is_after_to_in_date_range() {
        // given
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = LocalDateTime.now().minusHours(1);

        when(deviceRepository.findById(9001L)).thenReturn(Optional.of(device));

        // when / then
        assertThatThrownBy(() -> underTest.getDeviceTelemetry(9001L, from, to, 0, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("from");
    }

    @Test
    @DisplayName("getDeviceTelemetry throws ResourceNotFoundException when device not found")
    void should_throw_resource_not_found_when_device_not_found_on_telemetry_request() {
        // given
        when(deviceRepository.findById(9999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.getDeviceTelemetry(9999L, null, null, 0, 20))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");
    }

    // ─── toSummaryResponse ────────────────────────────────────────────────────

    @Test
    @DisplayName("toSummaryResponse maps device fields correctly including store and unit names")
    void should_map_device_fields_when_toSummaryResponse_called() {
        // given
        when(telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(9001L))
                .thenReturn(Optional.of(telemetryRecord));

        // when
        IotDeviceSummaryResponse result = underTest.toSummaryResponse(device);

        // then
        assertThat(result.getDeviceId()).isEqualTo(9001L);
        assertThat(result.getStoreName()).isEqualTo("Supermart Dallas");
        assertThat(result.getUnitName()).isEqualTo("Freezer-Aisle-3");
        assertThat(result.getStatus()).isEqualTo(DeviceStatus.ACTIVE);
    }

    // ─── toTelemetryResponse ──────────────────────────────────────────────────

    @Test
    @DisplayName("toTelemetryResponse maps telemetry record fields correctly")
    void should_map_telemetry_record_fields_when_toTelemetryResponse_called() {
        // when
        TelemetryResponse result = underTest.toTelemetryResponse(telemetryRecord);

        // then
        assertThat(result.getTelemetryId()).isEqualTo(78234441L);
        assertThat(result.getDeviceId()).isEqualTo(9001L);
        assertThat(result.getTemperature()).isEqualTo(-14.8);
        assertThat(result.getIsAlert()).isFalse();
    }
}
