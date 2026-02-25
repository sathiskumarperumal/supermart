package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.TelemetryIngestRequest;
import com.supermart.iot.dto.response.TelemetryResponse;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Incident;
import com.supermart.iot.entity.TelemetryRecord;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import com.supermart.iot.exception.BadRequestException;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final IotDeviceRepository deviceRepository;
    private final IncidentRepository incidentRepository;

    @Value("${app.telemetry.rate-limit-per-minute}")
    private int rateLimitPerMinute;

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

        // Auto-create incident if threshold exceeded and no open incident
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
                incidentRepository.save(incident);
                log.info("Auto-created incident for device {} — temp {} exceeded threshold [{}, {}]",
                        device.getDeviceId(), request.getTemperature(),
                        device.getMinTempThreshold(), device.getMaxTempThreshold());
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
