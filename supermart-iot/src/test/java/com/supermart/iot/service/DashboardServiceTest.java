package com.supermart.iot.service;

import com.supermart.iot.dto.response.DashboardSummaryResponse;
import com.supermart.iot.dto.response.IotDeviceSummaryResponse;
import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.entity.EquipmentUnit;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Store;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.EquipmentType;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.StoreRepository;
import com.supermart.iot.repository.TelemetryRepository;
import com.supermart.iot.service.impl.DashboardService;
import com.supermart.iot.service.impl.DeviceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private IotDeviceRepository deviceRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private TelemetryRepository telemetryRepository;
    @Mock private DeviceService deviceService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummary_returnsAggregatedCounts() {
        when(storeRepository.count()).thenReturn(5L);
        when(deviceRepository.countByStatus(DeviceStatus.ACTIVE)).thenReturn(12L);
        when(deviceRepository.countByStatus(DeviceStatus.FAULT)).thenReturn(3L);
        when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(2L);
        when(telemetryRepository.countByRecordedAtAfterAndIsAlertTrue(any())).thenReturn(7L);

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertThat(response.getTotalStores()).isEqualTo(5L);
        assertThat(response.getActiveDevices()).isEqualTo(12L);
        assertThat(response.getFaultyDevices()).isEqualTo(3L);
        assertThat(response.getOpenIncidents()).isEqualTo(2L);
        assertThat(response.getAlertsLastHour()).isEqualTo(7L);
        assertThat(response.getAsOf()).isNotNull();
    }

    @Test
    void getSummary_zeroValues_returnsZeroCounts() {
        when(storeRepository.count()).thenReturn(0L);
        when(deviceRepository.countByStatus(DeviceStatus.ACTIVE)).thenReturn(0L);
        when(deviceRepository.countByStatus(DeviceStatus.FAULT)).thenReturn(0L);
        when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(0L);
        when(telemetryRepository.countByRecordedAtAfterAndIsAlertTrue(any())).thenReturn(0L);

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertThat(response.getTotalStores()).isZero();
        assertThat(response.getActiveDevices()).isZero();
    }

    @Test
    void getAlerts_returnsPagedAlertDevices() {
        Store store = Store.builder().storeId(1L).storeName("Store A").build();
        EquipmentUnit unit = EquipmentUnit.builder()
                .unitId(1L).store(store).unitType(EquipmentType.REFRIGERATOR)
                .unitName("Unit A").createdAt(LocalDateTime.now()).build();
        IotDevice faultDevice = IotDevice.builder()
                .deviceId(10L).deviceSerial("DEV-001").deviceKey("key-001")
                .minTempThreshold(0.0).maxTempThreshold(10.0)
                .status(DeviceStatus.FAULT).unit(unit).build();

        IotDeviceSummaryResponse summary = IotDeviceSummaryResponse.builder()
                .deviceId(10L).deviceSerial("DEV-001").status(DeviceStatus.FAULT)
                .storeName("Store A").unitName("Unit A").isAlert(true).build();

        Page<IotDevice> page = new PageImpl<>(List.of(faultDevice));
        when(deviceRepository.findAllAlertDevices(any())).thenReturn(page);
        when(deviceService.toSummaryResponse(faultDevice)).thenReturn(summary);

        PagedResponse<IotDeviceSummaryResponse> result = dashboardService.getAlerts(0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeviceId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).getIsAlert()).isTrue();
    }
}
