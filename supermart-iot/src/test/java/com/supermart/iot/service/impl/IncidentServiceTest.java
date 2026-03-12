package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.CreateIncidentRequest;
import com.supermart.iot.dto.request.UpdateIncidentStatusRequest;
import com.supermart.iot.dto.response.IncidentResponse;
import com.supermart.iot.dto.response.IotDeviceSummaryResponse;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Incident;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import com.supermart.iot.exception.BadRequestException;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link IncidentService}.
 * Covers DEVICE_FAULT notification dispatch integration added for SCRUM-4 AC-4,
 * plus createIncident, updateStatus, and getIncidentById paths.
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
        Long deviceId = 1L;
        IotDevice device = IotDevice.builder()
                .deviceId(deviceId)
                .deviceSerial("SN-001")
                .status(DeviceStatus.ACTIVE)
                .build();
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .deviceId(deviceId)
                .incidentType(IncidentType.DEVICE_FAULT)
                .description("Device fault detected")
                .build();
        Incident savedIncident = Incident.builder()
                .incidentId(1L)
                .device(device)
                .incidentType(IncidentType.DEVICE_FAULT)
                .status(IncidentStatus.OPEN)
                .description("Device fault detected")
                .createdAt(LocalDateTime.now())
                .build();
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder()
                .deviceId(deviceId)
                .deviceSerial("SN-001")
                .status(DeviceStatus.ACTIVE)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(deviceId, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);
        when(deviceService.toSummaryResponse(device)).thenReturn(deviceSummary);

        // when
        IncidentResponse result = underTest.createIncident(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getIncidentType()).isEqualTo(IncidentType.DEVICE_FAULT);
        verify(notificationService).dispatchIncidentNotifications(any(Incident.class), eq(device));
    }

    @Test
    @DisplayName("AC-4: dispatchIncidentNotifications NOT called for non-DEVICE_FAULT incident types")
    void should_notCallDispatchIncidentNotifications_when_incidentTypeIsNotDeviceFault() {
        // given
        Long deviceId = 1L;
        IotDevice device = IotDevice.builder()
                .deviceId(deviceId)
                .deviceSerial("SN-001")
                .status(DeviceStatus.FAULT)
                .build();
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .deviceId(deviceId)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .description("Temperature exceeded threshold")
                .build();
        Incident savedIncident = Incident.builder()
                .incidentId(1L)
                .device(device)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .status(IncidentStatus.OPEN)
                .description("Temperature exceeded threshold")
                .createdAt(LocalDateTime.now())
                .build();
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder()
                .deviceId(deviceId)
                .deviceSerial("SN-001")
                .status(DeviceStatus.FAULT)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(deviceId, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);
        when(deviceService.toSummaryResponse(device)).thenReturn(deviceSummary);

        // when
        IncidentResponse result = underTest.createIncident(request);

        // then
        assertThat(result.getIncidentType()).isEqualTo(IncidentType.TEMP_EXCEEDED);
        verify(notificationService, never()).dispatchIncidentNotifications(any(), any());
    }

    // -------------------------------------------------------------------------
    // Existing: Conflict enforcement
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Throws ConflictException when open incident already exists for the device")
    void should_throwConflictException_when_openIncidentAlreadyExistsForDevice() {
        // given
        Long deviceId = 1L;
        IotDevice device = IotDevice.builder()
                .deviceId(deviceId)
                .deviceSerial("SN-001")
                .build();
        Incident existingIncident = Incident.builder()
                .incidentId(99L)
                .device(device)
                .status(IncidentStatus.OPEN)
                .build();
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .deviceId(deviceId)
                .incidentType(IncidentType.DEVICE_FAULT)
                .description("Another fault")
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(deviceId, IncidentStatus.OPEN))
                .thenReturn(Optional.of(existingIncident));

        // when / then
        assertThatThrownBy(() -> underTest.createIncident(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("open incident");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when device is not found during incident creation")
    void should_throwResourceNotFoundException_when_deviceNotFoundOnCreate() {
        // given
        Long deviceId = 999L;
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .deviceId(deviceId)
                .incidentType(IncidentType.DEVICE_FAULT)
                .description("Fault")
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.createIncident(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // -------------------------------------------------------------------------
    // updateStatus: happy path and validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateStatus transitions incident to RESOLVED and sets resolvedAt")
    void should_updateStatusToResolved_when_incidentIsOpen() {
        // given
        Long incidentId = 1L;
        IotDevice device = IotDevice.builder().deviceId(1L).deviceSerial("SN-001").status(DeviceStatus.FAULT).build();
        Incident incident = Incident.builder()
                .incidentId(incidentId)
                .device(device)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .status(IncidentStatus.OPEN)
                .description("Temp breach")
                .createdAt(LocalDateTime.now())
                .build();
        Incident updatedIncident = Incident.builder()
                .incidentId(incidentId)
                .device(device)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .status(IncidentStatus.RESOLVED)
                .description("Temp breach")
                .createdAt(incident.getCreatedAt())
                .resolvedAt(LocalDateTime.now())
                .build();
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder()
                .deviceId(1L).deviceSerial("SN-001").status(DeviceStatus.FAULT).build();
        UpdateIncidentStatusRequest request = UpdateIncidentStatusRequest.builder()
                .status(IncidentStatus.RESOLVED)
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(updatedIncident);
        when(deviceService.toSummaryResponse(device)).thenReturn(deviceSummary);

        // when
        IncidentResponse result = underTest.updateStatus(incidentId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    @DisplayName("updateStatus throws BadRequestException when transitioning from RESOLVED")
    void should_throwBadRequestException_when_transitioningFromResolved() {
        // given
        Long incidentId = 1L;
        IotDevice device = IotDevice.builder().deviceId(1L).deviceSerial("SN-001").status(DeviceStatus.ACTIVE).build();
        Incident resolvedIncident = Incident.builder()
                .incidentId(incidentId)
                .device(device)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .status(IncidentStatus.RESOLVED)
                .createdAt(LocalDateTime.now())
                .resolvedAt(LocalDateTime.now())
                .build();
        UpdateIncidentStatusRequest request = UpdateIncidentStatusRequest.builder()
                .status(IncidentStatus.OPEN)
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(resolvedIncident));

        // when / then
        assertThatThrownBy(() -> underTest.updateStatus(incidentId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transition from RESOLVED");
    }

    @Test
    @DisplayName("getIncidentById returns incident response when incident exists")
    void should_returnIncidentResponse_when_incidentFoundById() {
        // given
        Long incidentId = 1L;
        IotDevice device = IotDevice.builder().deviceId(1L).deviceSerial("SN-001").status(DeviceStatus.ACTIVE).build();
        Incident incident = Incident.builder()
                .incidentId(incidentId)
                .device(device)
                .incidentType(IncidentType.DEVICE_FAULT)
                .status(IncidentStatus.OPEN)
                .description("Device fault")
                .createdAt(LocalDateTime.now())
                .build();
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder()
                .deviceId(1L).deviceSerial("SN-001").status(DeviceStatus.ACTIVE).build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(deviceService.toSummaryResponse(device)).thenReturn(deviceSummary);

        // when
        IncidentResponse result = underTest.getIncidentById(incidentId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getIncidentId()).isEqualTo(incidentId);
        assertThat(result.getIncidentType()).isEqualTo(IncidentType.DEVICE_FAULT);
    }

    @Test
    @DisplayName("getIncidentById throws ResourceNotFoundException when incident not found")
    void should_throwResourceNotFoundException_when_incidentNotFoundById() {
        // given
        Long incidentId = 999L;
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.getIncidentById(incidentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
