package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.DashboardSummaryResponse;
import com.supermart.iot.dto.response.IotDeviceSummaryResponse;
import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.entity.EquipmentUnit;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Store;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.StoreRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DashboardService}.
 *
 * <p>Covers dashboard summary aggregation and alert device listing.</p>
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private DashboardService underTest;

    private IotDevice alertDevice;

    @BeforeEach
    void setUp() {
        Store store = Store.builder().storeId(1001L).storeName("Supermart Dallas").build();
        EquipmentUnit unit = EquipmentUnit.builder().unitId(501L).store(store).unitName("Freezer-1").build();

        alertDevice = IotDevice.builder()
                .deviceId(9001L)
                .unit(unit)
                .deviceSerial("DEV-9001")
                .status(DeviceStatus.FAULT)
                .build();
    }

    // ─── getSummary ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSummary returns correctly aggregated dashboard summary")
    void should_return_dashboard_summary_with_correct_counts() {
        // given
        when(storeRepository.count()).thenReturn(3L);
        when(deviceRepository.countByStatus(DeviceStatus.ACTIVE)).thenReturn(4L);
        when(deviceRepository.countByStatus(DeviceStatus.FAULT)).thenReturn(1L);
        when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(2L);
        when(telemetryRepository.countByRecordedAtAfterAndIsAlertTrue(any(LocalDateTime.class))).thenReturn(5L);

        // when
        DashboardSummaryResponse result = underTest.getSummary();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalStores()).isEqualTo(3L);
        assertThat(result.getActiveDevices()).isEqualTo(4L);
        assertThat(result.getFaultyDevices()).isEqualTo(1L);
        assertThat(result.getOpenIncidents()).isEqualTo(2L);
        assertThat(result.getAlertsLastHour()).isEqualTo(5L);
        assertThat(result.getAsOf()).isNotNull();
    }

    // ─── getAlerts ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAlerts returns paged alert device summaries")
    void should_return_paged_alert_devices_when_getAlerts_called() {
        // given
        Page<IotDevice> page = new PageImpl<>(List.of(alertDevice));
        IotDeviceSummaryResponse summaryResponse = IotDeviceSummaryResponse.builder()
                .deviceId(9001L)
                .status(DeviceStatus.FAULT)
                .isAlert(true)
                .build();

        when(deviceRepository.findAllAlertDevices(any(Pageable.class))).thenReturn(page);
        when(deviceService.toSummaryResponse(alertDevice)).thenReturn(summaryResponse);

        // when
        PagedResponse<IotDeviceSummaryResponse> result = underTest.getAlerts(0, 20);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeviceId()).isEqualTo(9001L);
        assertThat(result.getContent().get(0).getIsAlert()).isTrue();
    }
}
