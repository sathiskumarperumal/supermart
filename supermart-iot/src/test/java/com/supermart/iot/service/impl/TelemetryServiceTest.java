package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.TelemetryIngestRequest;
import com.supermart.iot.dto.response.TelemetryResponse;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Incident;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.exception.RateLimitException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.TelemetryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TelemetryService}.
 * Covers notification trigger integration added for SCRUM-4.
 */
@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TelemetryService underTest;

    // -------------------------------------------------------------------------
    // AC-1: Notification dispatch triggered on new threshold breach incident
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-1: dispatchIncidentNotifications called when new TEMP_EXCEEDED incident is created")
    void should_callDispatchIncidentNotifications_when_newTemperatureBreachIncidentCreated() {
        // given

        // when

        // then
        // TODO: implement assertions — verify notificationService.dispatchIncidentNotifications() invoked
    }

    // -------------------------------------------------------------------------
    // AC-3: No duplicate notification when open incident already exists
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-3: dispatchIncidentNotifications NOT called when open incident already exists")
    void should_notCallDispatchIncidentNotifications_when_openIncidentAlreadyExists() {
        // given

        // when

        // then
        // TODO: implement assertions — verify notificationService is never called
    }

    // -------------------------------------------------------------------------
    // Existing: Rate limit enforcement
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Throws RateLimitException when device exceeds per-minute submission rate")
    void should_throwRateLimitException_when_deviceExceedsRateLimit() {
        // given

        // when

        // then
        // TODO: implement assertions
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when device ID is not found")
    void should_throwResourceNotFoundException_when_deviceNotFound() {
        // given

        // when

        // then
        // TODO: implement assertions
    }
}
