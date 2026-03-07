package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.DashboardSummaryResponse;
import com.supermart.iot.dto.response.IotDeviceSummaryResponse;
import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.EquipmentUnit;
import com.supermart.iot.entity.Store;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.EquipmentType;
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
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private IotDeviceRepository deviceRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private TelemetryRepository telemetryRepository;
    @Mock private DeviceService deviceService;

    @InjectMocks
    private DashboardService underTest;

    private IotDevice device;

    @BeforeEach
    void setUp() {
        Store store = Store.builder()
                .storeId(1L).storeName("Store A").storeCode("SA01")
                .address("1 Main St").city("Austin").state("TX").createdAt(LocalDateTime.now())
                .build();
        EquipmentUnit unit = EquipmentUnit.builder()
                .unitId(10L).store(store).unitType(EquipmentType.FREEZER)
                .unitName("Freezer 1").locationDesc("Back").createdAt(LocalDateTime.now())
                .build();
        device = IotDevice.builder()
                .deviceId(100L).deviceSerial("SN-100").deviceKey("k-100")
                .minTempThreshold(-20.0).maxTempThreshold(4.0)
                .status(DeviceStatus.FAULT).unit(unit).lastSeenAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getSummary returns aggregated counts from all repositories")
    void should_returnSummary_when_getSummaryCalled() {
        when(storeRepository.count()).thenReturn(5L);
        when(deviceRepository.countByStatus(DeviceStatus.ACTIVE)).thenReturn(20L);
        when(deviceRepository.countByStatus(DeviceStatus.FAULT)).thenReturn(3L);
        when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(7L);
        when(telemetryRepository.countByRecordedAtAfterAndIsAlertTrue(any())).thenReturn(12L);

        DashboardSummaryResponse result = underTest.getSummary();

        assertThat(result.getTotalStores()).isEqualTo(5L);
        assertThat(result.getActiveDevices()).isEqualTo(20L);
        assertThat(result.getFaultyDevices()).isEqualTo(3L);
        assertThat(result.getOpenIncidents()).isEqualTo(7L);
        assertThat(result.getAlertsLastHour()).isEqualTo(12L);
        assertThat(result.getAsOf()).isNotNull();
    }

    @Test
    @DisplayName("getAlerts returns paged summary of alert devices")
    void should_returnPagedAlerts_when_alertDevicesExist() {
        IotDeviceSummaryResponse summary = IotDeviceSummaryResponse.builder()
                .deviceId(100L).status(DeviceStatus.FAULT).isAlert(true).build();
        when(deviceRepository.findAllAlertDevices(any())).thenReturn(new PageImpl<>(List.of(device)));
        when(deviceService.toSummaryResponse(device)).thenReturn(summary);

        PagedResponse<IotDeviceSummaryResponse> result = underTest.getAlerts(0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeviceId()).isEqualTo(100L);
        assertThat(result.getContent().get(0).getIsAlert()).isTrue();
    }
}
