package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.TelemetryIngestRequest;
import com.supermart.iot.dto.response.TelemetryResponse;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Incident;
import com.supermart.iot.entity.TelemetryRecord;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import com.supermart.iot.exception.RateLimitException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for ingesting IoT telemetry readings and triggering incident creation
 * with automated alert notifications when temperature thresholds are exceeded.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final IotDeviceRepository deviceRepository;
    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;

    @Value("${app.telemetry.rate-limit-per-minute}")
    private int rateLimitPerMinute;

    /**
     * Ingests a telemetry reading from an IoT device, evaluates threshold breaches,
     * persists the record, and — when a new temperature breach incident is created —
     * dispatches automated Email/SMS alerts to all configured Store Managers.
     *
     * <p>De-duplication: if an OPEN incident already exists for the device, no new
     * incident is created and no notification is dispatched (AC-3).
     *
     * @param request the telemetry payload containing deviceId, temperature, and timestamp
     * @return {@link TelemetryResponse} with the persisted record details and alert flag
     * @throws ResourceNotFoundException if the device ID is not found
     * @throws RateLimitException        if the device has exceeded the per-minute submission limit
     */
    @Transactional
    public TelemetryResponse ingest(TelemetryIngestRequest request) {
        IotDevice device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "IoT device with id " + request.getDeviceId() + " not found."));

        // Rate limit check
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentCount = telemetryRepository.countByDevice_DeviceIdAndRecordedAtAfter(
                device.getDeviceId(), oneMinuteAgo);
        if (recentCount >= rateLimitPerMinute) {
            throw new RateLimitException("Device " + device.getDeviceId() +
                    " has exceeded the telemetry submission rate limit. Max " + rateLimitPerMinute + " requests per minute.");
        }

        // Evaluate threshold
        boolean isAlert = request.getTemperature() < device.getMinTempThreshold()
                || request.getTemperature() > device.getMaxTempThreshold();

        // Persist telemetry
        TelemetryRecord record = TelemetryRecord.builder()
                .device(device)
                .temperature(request.getTemperature())
                .recordedAt(request.getRecordedAt())
                .isAlert(isAlert)
                .build();
        record = telemetryRepository.save(record);

        // Update device lastSeenAt
        device.setLastSeenAt(LocalDateTime.now());
        if (isAlert) {
            device.setStatus(DeviceStatus.FAULT);
        }
        deviceRepository.save(device);

        // Auto-create incident if threshold exceeded and no open incident exists (AC-3)
        if (isAlert) {
            Optional<Incident> existing = incidentRepository.findByDevice_DeviceIdAndStatus(
                    device.getDeviceId(), IncidentStatus.OPEN);
            if (existing.isEmpty()) {
                Incident incident = Incident.builder()
                        .device(device)
                        .incidentType(IncidentType.TEMP_EXCEEDED)
                        .status(IncidentStatus.OPEN)
                        .description(buildIncidentDescription(device, request.getTemperature()))
                        .createdAt(LocalDateTime.now())
                        .build();
                incident = incidentRepository.save(incident);
                log.info("Auto-created incident for device {} — temp {} exceeded threshold [{}, {}]",
                        device.getDeviceId(), request.getTemperature(),
                        device.getMinTempThreshold(), device.getMaxTempThreshold());

                // AC-1: Dispatch Email/SMS notifications upon new incident creation
                notificationService.dispatchIncidentNotifications(incident, device);
            } else {
                // AC-3: Open incident already exists — suppress duplicate notification
                log.debug("Open incident already exists for deviceId={} — notification suppressed",
                        device.getDeviceId());
            }
        }

        return TelemetryResponse.builder()
                .telemetryId(record.getTelemetryId())
                .deviceId(device.getDeviceId())
                .temperature(record.getTemperature())
                .recordedAt(record.getRecordedAt())
                .isAlert(record.getIsAlert())
                .build();
    }

    /**
     * Builds a human-readable incident description based on the threshold violation.
     *
     * @param device      the IoT device whose threshold was violated
     * @param temperature the recorded temperature value
     * @return formatted incident description string
     */
    private String buildIncidentDescription(IotDevice device, Double temperature) {
        if (temperature > device.getMaxTempThreshold()) {
            return String.format("Temperature exceeded max threshold of %.1f°C. Recorded: %.1f°C",
                    device.getMaxTempThreshold(), temperature);
        } else {
            return String.format("Temperature below min threshold of %.1f°C. Recorded: %.1f°C",
                    device.getMinTempThreshold(), temperature);
        }
    }
}
