package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.DashboardSummaryResponse;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.StoreRepository;
import com.supermart.iot.repository.TelemetryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DashboardService}.
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

    @Test
    @DisplayName("getSummary returns correct counts from repositories")
    void should_returnDashboardSummary_when_getSummaryCalled() {
        // given
        when(storeRepository.count()).thenReturn(5L);
        when(deviceRepository.countByStatus(DeviceStatus.ACTIVE)).thenReturn(20L);
        when(deviceRepository.countByStatus(DeviceStatus.FAULT)).thenReturn(3L);
        when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(7L);
        when(telemetryRepository.countByRecordedAtAfterAndIsAlertTrue(any(LocalDateTime.class))).thenReturn(12L);

        // when
        DashboardSummaryResponse result = underTest.getSummary();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalStores()).isEqualTo(5L);
        assertThat(result.getActiveDevices()).isEqualTo(20L);
        assertThat(result.getFaultyDevices()).isEqualTo(3L);
        assertThat(result.getOpenIncidents()).isEqualTo(7L);
        assertThat(result.getAlertsLastHour()).isEqualTo(12L);
        assertThat(result.getAsOf()).isNotNull();
    }
}
