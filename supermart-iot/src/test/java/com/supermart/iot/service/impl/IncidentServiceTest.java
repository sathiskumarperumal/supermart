package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.CreateIncidentRequest;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Incident;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import com.supermart.iot.exception.ConflictException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.TechnicianAssignmentRepository;
import com.supermart.iot.repository.TechnicianRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link IncidentService}.
 * Covers DEVICE_FAULT notification dispatch integration added for SCRUM-4 AC-4.
 */
@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private TechnicianAssignmentRepository assignmentRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private IncidentService underTest;

    // -------------------------------------------------------------------------
    // AC-4: Notification sent on DEVICE_FAULT auto-creation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-4: dispatchIncidentNotifications called when DEVICE_FAULT incident is created")
    void should_callDispatchIncidentNotifications_when_deviceFaultIncidentCreated() {
        // given

        // when

        // then
        // TODO: implement assertions — verify notificationService.dispatchIncidentNotifications() called
    }

    @Test
    @DisplayName("AC-4: dispatchIncidentNotifications NOT called for non-DEVICE_FAULT incident types")
    void should_notCallDispatchIncidentNotifications_when_incidentTypeIsNotDeviceFault() {
        // given

        // when

        // then
        // TODO: implement assertions — verify notificationService is never called for TEMP_EXCEEDED
    }

    // -------------------------------------------------------------------------
    // Existing: Conflict enforcement
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Throws ConflictException when open incident already exists for the device")
    void should_throwConflictException_when_openIncidentAlreadyExistsForDevice() {
        // given

        // when

        // then
        // TODO: implement assertions
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when device is not found during incident creation")
    void should_throwResourceNotFoundException_when_deviceNotFoundOnCreate() {
        // given

        // when

        // then
        // TODO: implement assertions
    }
}
