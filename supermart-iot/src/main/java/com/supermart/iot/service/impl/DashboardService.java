package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.DashboardSummaryResponse;
import com.supermart.iot.dto.response.IotDeviceSummaryResponse;
import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.StoreRepository;
import com.supermart.iot.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StoreRepository storeRepository;
    private final IotDeviceRepository deviceRepository;
    private final IncidentRepository incidentRepository;
    private final TelemetryRepository telemetryRepository;
    private final DeviceService deviceService;

    public DashboardSummaryResponse getSummary() {
        return DashboardSummaryResponse.builder()
                .totalStores(storeRepository.count())
                .activeDevices(deviceRepository.countByStatus(DeviceStatus.ACTIVE))
                .faultyDevices(deviceRepository.countByStatus(DeviceStatus.FAULT))
                .openIncidents(incidentRepository.countByStatus(IncidentStatus.OPEN))
                .alertsLastHour(telemetryRepository.countByRecordedAtAfterAndIsAlertTrue(
                        LocalDateTime.now().minusHours(1)))
                .asOf(LocalDateTime.now())
                .build();
    }

    public PagedResponse<IotDeviceSummaryResponse> getAlerts(int page, int size) {
        return PagedResponse.of(
                deviceRepository.findAllAlertDevices(PageRequest.of(page, size))
                        .map(deviceService::toSummaryResponse));
    }
}
