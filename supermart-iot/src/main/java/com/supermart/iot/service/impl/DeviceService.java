package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.*;
import com.supermart.iot.entity.DeviceDecommissionAudit;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.TelemetryRecord;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.exception.ConflictException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.DeviceDecommissionAuditRepository;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service layer for IoT device operations including listing, detail retrieval,
 * telemetry pagination, and device decommissioning.
 *
 * <p>Decommissioning is a soft-delete: the device status is set to
 * {@link DeviceStatus#DECOMMISSIONED} and {@code decommissionedAt} is stamped.
 * An audit record is written to {@code device_threshold_audit}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final IotDeviceRepository deviceRepository;
    private final TelemetryRepository telemetryRepository;
    private final IncidentRepository incidentRepository;
    private final DeviceDecommissionAuditRepository auditRepository;

    /**
     * Returns a paginated list of devices, optionally filtered by store and/or status.
     * DECOMMISSIONED devices are excluded by default unless
     * {@code includeDecommissioned} is {@code true} (AC-2, AC-3).
     *
     * @param storeId               optional store filter
     * @param status                optional device status filter
     * @param includeDecommissioned when {@code true}, includes DECOMMISSIONED devices
     * @param page                  zero-based page index
     * @param size                  page size
     * @return paged summary responses
     */
    public PagedResponse<IotDeviceSummaryResponse> listDevices(Long storeId, DeviceStatus status,
                                                               boolean includeDecommissioned,
                                                               int page, int size) {
        Page<IotDevice> devicePage = deviceRepository.findByStoreIdAndStatus(
                storeId, status, includeDecommissioned, PageRequest.of(page, size));
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
                .decommissionedAt(device.getDecommissionedAt())
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

    /**
     * Soft-deletes a device by marking it as DECOMMISSIONED (AC-1, AC-4, AC-5, AC-6).
     *
     * <p>Rules enforced:
     * <ul>
     *   <li>The device must exist — throws {@link ResourceNotFoundException} if not.</li>
     *   <li>The device must not have an OPEN incident — throws {@link ConflictException}
     *       with HTTP 409 if one exists (AC-4).</li>
     *   <li>Sets {@code status=DECOMMISSIONED} and {@code decommissionedAt=now()} (AC-1).</li>
     *   <li>Writes an audit record to {@code device_threshold_audit} (AC-6).</li>
     * </ul>
     *
     * @param deviceId    the ID of the device to decommission
     * @param performedBy the email of the ADMIN principal performing the action
     * @throws ResourceNotFoundException if no device exists with the given ID
     * @throws ConflictException         if the device has an OPEN incident
     */
    @Transactional
    public void decommissionDevice(Long deviceId, String performedBy) {
        log.info("Decommission requested: deviceId={}, performedBy={}", deviceId, performedBy);

        IotDevice device = findDeviceOrThrow(deviceId);

        incidentRepository.findByDevice_DeviceIdAndStatus(deviceId, IncidentStatus.OPEN)
                .ifPresent(incident -> {
                    throw new ConflictException(
                            "Cannot decommission device " + deviceId +
                            ": open incident " + incident.getIncidentId() + " must be resolved first.");
                });

        device.setStatus(DeviceStatus.DECOMMISSIONED);
        device.setDecommissionedAt(LocalDateTime.now());
        deviceRepository.save(device);

        DeviceDecommissionAudit audit = DeviceDecommissionAudit.builder()
                .deviceId(deviceId)
                .eventType("DECOMMISSIONED")
                .performedBy(performedBy)
                .createdAt(LocalDateTime.now())
                .build();
        auditRepository.save(audit);

        log.info("Device decommissioned: deviceId={}, performedBy={}", deviceId, performedBy);
    }

    /**
     * Retrieves an {@link IotDevice} by ID or throws {@link ResourceNotFoundException}.
     *
     * @param deviceId the device ID to look up
     * @return the found device
     * @throws ResourceNotFoundException if no device exists with the given ID
     */
    public IotDevice findDeviceOrThrow(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("IoT device with id " + deviceId + " not found."));
    }
}
