package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.*;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.TelemetryRecord;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final IotDeviceRepository deviceRepository;
    private final TelemetryRepository telemetryRepository;

    public PagedResponse<IotDeviceSummaryResponse> listDevices(Long storeId, DeviceStatus status, int page, int size) {
        Page<IotDevice> devicePage = deviceRepository.findByStoreIdAndStatus(storeId, status, PageRequest.of(page, size));
        return PagedResponse.of(devicePage.map(this::toSummaryResponse));
    }

    public IotDeviceResponse getDeviceById(Long deviceId) {
        IotDevice device = findDeviceOrThrow(deviceId);
        return toDetailResponse(device);
    }

    public PagedResponse<TelemetryResponse> getDeviceTelemetry(Long deviceId, LocalDateTime from, LocalDateTime to, int page, int size) {
        findDeviceOrThrow(deviceId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new com.supermart.iot.exception.BadRequestException("'from' date must be before 'to' date.");
        }
        Page<TelemetryRecord> records = telemetryRepository.findByDeviceIdAndDateRange(deviceId, from, to, PageRequest.of(page, size));
        return PagedResponse.of(records.map(this::toTelemetryResponse));
    }

    public IotDeviceSummaryResponse toSummaryResponse(IotDevice device) {
        Optional<TelemetryRecord> latest = telemetryRepository.findTopByDevice_DeviceIdOrderByRecordedAtDesc(device.getDeviceId());
        return IotDeviceSummaryResponse.builder()
                .deviceId(device.getDeviceId())
                .deviceSerial(device.getDeviceSerial())
                .status(device.getStatus())
                .lastSeenAt(device.getLastSeenAt())
                .storeName(device.getUnit().getStore().getStoreName())
                .unitName(device.getUnit().getUnitName())
                .latestTemperature(latest.map(TelemetryRecord::getTemperature).orElse(null))
                .isAlert(latest.map(TelemetryRecord::getIsAlert).orElse(false))
                .build();
    }

    private IotDeviceResponse toDetailResponse(IotDevice device) {
        return IotDeviceResponse.builder()
                .deviceId(device.getDeviceId())
                .unitId(device.getUnit().getUnitId())
                .deviceSerial(device.getDeviceSerial())
                .minTempThreshold(device.getMinTempThreshold())
                .maxTempThreshold(device.getMaxTempThreshold())
                .status(device.getStatus())
                .lastSeenAt(device.getLastSeenAt())
                .unit(EquipmentUnitResponse.builder()
                        .unitId(device.getUnit().getUnitId())
                        .storeId(device.getUnit().getStore().getStoreId())
                        .unitType(device.getUnit().getUnitType())
                        .unitName(device.getUnit().getUnitName())
                        .locationDesc(device.getUnit().getLocationDesc())
                        .createdAt(device.getUnit().getCreatedAt())
                        .build())
                .build();
    }

    public TelemetryResponse toTelemetryResponse(TelemetryRecord record) {
        return TelemetryResponse.builder()
                .telemetryId(record.getTelemetryId())
                .deviceId(record.getDevice().getDeviceId())
                .temperature(record.getTemperature())
                .recordedAt(record.getRecordedAt())
                .isAlert(record.getIsAlert())
                .build();
    }

    public IotDevice findDeviceOrThrow(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("IoT device with id " + deviceId + " not found."));
    }
}
