package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.AssignTechnicianRequest;
import com.supermart.iot.dto.request.CreateIncidentRequest;
import com.supermart.iot.dto.request.UpdateIncidentStatusRequest;
import com.supermart.iot.dto.response.IncidentResponse;
import com.supermart.iot.dto.response.IotDeviceSummaryResponse;
import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.TechnicianAssignmentResponse;
import com.supermart.iot.entity.*;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import com.supermart.iot.exception.BadRequestException;
import com.supermart.iot.exception.ConflictException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link IncidentService}.
 *
 * <p>Covers incident creation, retrieval, status updates, technician assignment,
 * and all error-path branches including SONAR CRITICAL fix (NOT_FOUND_SUFFIX constant).</p>
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

    @InjectMocks
    private IncidentService underTest;

    private IotDevice device;
    private Incident incident;
    private Technician technician;

    @BeforeEach
    void setUp() {
        Store store = Store.builder().storeId(1001L).storeName("Store A").build();
        com.supermart.iot.entity.EquipmentUnit unit = com.supermart.iot.entity.EquipmentUnit.builder()
                .unitId(501L).store(store).unitName("Freezer-1").build();

        device = IotDevice.builder()
                .deviceId(9001L)
                .unit(unit)
                .deviceSerial("DEV-9001")
                .deviceKey("key-9001")
                .status(DeviceStatus.ACTIVE)
                .minTempThreshold(-25.0)
                .maxTempThreshold(-15.0)
                .build();

        incident = Incident.builder()
                .incidentId(3301L)
                .device(device)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .status(IncidentStatus.OPEN)
                .description("Temperature exceeded threshold.")
                .createdAt(LocalDateTime.now())
                .build();

        technician = Technician.builder()
                .technicianId(55L)
                .fullName("Carlos Rivera")
                .email("c.rivera@hvac.com")
                .phone("+1-214-555-0192")
                .region("Texas South")
                .build();
    }

    // ─── listIncidents ────────────────────────────────────────────────────────

    @Test
    @DisplayName("listIncidents returns paged response with mapped incidents")
    void should_return_paged_incidents_when_listIncidents_called() {
        // given
        Page<Incident> page = new PageImpl<>(List.of(incident));
        when(incidentRepository.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(page);
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder().deviceId(9001L).build();
        when(deviceService.toSummaryResponse(device)).thenReturn(deviceSummary);

        // when
        PagedResponse<IncidentResponse> result = underTest.listIncidents(null, null, null, 0, 20);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    // ─── createIncident ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createIncident returns saved incident response when device exists and no open incident")
    void should_return_incident_response_when_incident_created_successfully() {
        // given
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setDeviceId(9001L);
        request.setIncidentType(IncidentType.TEMP_EXCEEDED);
        request.setDescription("Temp too high.");

        when(deviceRepository.findById(9001L)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(9001L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder().deviceId(9001L).build();
        when(deviceService.toSummaryResponse(device)).thenReturn(deviceSummary);

        // when
        IncidentResponse result = underTest.createIncident(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo(9001L);
        verify(incidentRepository, times(1)).save(any(Incident.class));
    }

    @Test
    @DisplayName("createIncident throws ResourceNotFoundException when device does not exist")
    void should_throw_resource_not_found_when_device_missing_on_create() {
        // given
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setDeviceId(9999L);
        request.setIncidentType(IncidentType.TEMP_EXCEEDED);

        when(deviceRepository.findById(9999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.createIncident(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999")
                .hasMessageContaining("not found.");
    }

    @Test
    @DisplayName("createIncident throws ConflictException when open incident already exists for device")
    void should_throw_conflict_when_open_incident_already_exists() {
        // given
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setDeviceId(9001L);
        request.setIncidentType(IncidentType.TEMP_EXCEEDED);

        when(deviceRepository.findById(9001L)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(9001L, IncidentStatus.OPEN))
                .thenReturn(Optional.of(incident));

        // when / then
        assertThatThrownBy(() -> underTest.createIncident(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("open incident");
    }

    // ─── getIncidentById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getIncidentById returns full incident response with assignments")
    void should_return_incident_with_assignments_when_found_by_id() {
        // given
        incident.setAssignments(Collections.emptyList());
        when(incidentRepository.findById(3301L)).thenReturn(Optional.of(incident));
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder().deviceId(9001L).build();
        when(deviceService.toSummaryResponse(device)).thenReturn(deviceSummary);

        // when
        IncidentResponse result = underTest.getIncidentById(3301L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getIncidentId()).isEqualTo(3301L);
    }

    @Test
    @DisplayName("getIncidentById throws ResourceNotFoundException when incident does not exist")
    void should_throw_resource_not_found_when_incident_not_found_by_id() {
        // given
        when(incidentRepository.findById(9999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.getIncidentById(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999")
                .hasMessageContaining("not found.");
    }

    // ─── updateStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus transitions incident to RESOLVED and sets resolvedAt")
    void should_set_resolved_at_when_status_updated_to_resolved() {
        // given
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest();
        request.setStatus(IncidentStatus.RESOLVED);

        incident.setAssignments(Collections.emptyList());
        when(incidentRepository.findById(3301L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder().deviceId(9001L).build();
        when(deviceService.toSummaryResponse(device)).thenReturn(deviceSummary);

        // when
        IncidentResponse result = underTest.updateStatus(3301L, request);

        // then
        assertThat(result).isNotNull();
        assertThat(incident.getResolvedAt()).isNotNull();
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
    }

    @Test
    @DisplayName("updateStatus throws BadRequestException when transitioning from RESOLVED")
    void should_throw_bad_request_when_transitioning_from_resolved_status() {
        // given
        incident.setStatus(IncidentStatus.RESOLVED);
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest();
        request.setStatus(IncidentStatus.OPEN);

        when(incidentRepository.findById(3301L)).thenReturn(Optional.of(incident));

        // when / then
        assertThatThrownBy(() -> underTest.updateStatus(3301L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("RESOLVED");
    }

    // ─── assignTechnician ─────────────────────────────────────────────────────

    @Test
    @DisplayName("assignTechnician returns assignment response when incident and technician exist")
    void should_return_assignment_response_when_technician_assigned_successfully() {
        // given
        AssignTechnicianRequest request = new AssignTechnicianRequest();
        request.setTechnicianId(55L);
        request.setNotes("Priority repair.");

        TechnicianAssignment assignment = TechnicianAssignment.builder()
                .assignmentId(1L)
                .incident(incident)
                .technician(technician)
                .assignedAt(LocalDateTime.now())
                .notes("Priority repair.")
                .build();

        when(incidentRepository.findById(3301L)).thenReturn(Optional.of(incident));
        when(technicianRepository.findById(55L)).thenReturn(Optional.of(technician));
        when(assignmentRepository.save(any(TechnicianAssignment.class))).thenReturn(assignment);
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        // when
        TechnicianAssignmentResponse result = underTest.assignTechnician(3301L, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTechnician().getFullName()).isEqualTo("Carlos Rivera");
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    @DisplayName("assignTechnician throws BadRequestException when incident is RESOLVED")
    void should_throw_bad_request_when_assigning_technician_to_resolved_incident() {
        // given
        incident.setStatus(IncidentStatus.RESOLVED);
        AssignTechnicianRequest request = new AssignTechnicianRequest();
        request.setTechnicianId(55L);

        when(incidentRepository.findById(3301L)).thenReturn(Optional.of(incident));

        // when / then
        assertThatThrownBy(() -> underTest.assignTechnician(3301L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("RESOLVED");
    }

    @Test
    @DisplayName("assignTechnician throws ResourceNotFoundException when technician does not exist")
    void should_throw_resource_not_found_when_technician_not_found_on_assign() {
        // given
        AssignTechnicianRequest request = new AssignTechnicianRequest();
        request.setTechnicianId(9999L);

        when(incidentRepository.findById(3301L)).thenReturn(Optional.of(incident));
        when(technicianRepository.findById(9999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.assignTechnician(3301L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999")
                .hasMessageContaining("not found.");
    }

    // ─── toResponse with assignments list ────────────────────────────────────

    @Test
    @DisplayName("toResponse includes assignments when includeAssignments is true and list is non-null")
    void should_include_assignments_when_include_assignments_true() {
        // given
        TechnicianAssignment assignment = TechnicianAssignment.builder()
                .assignmentId(1L)
                .incident(incident)
                .technician(technician)
                .assignedAt(LocalDateTime.now())
                .notes("Note")
                .build();
        incident.setAssignments(List.of(assignment));
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder().deviceId(9001L).build();
        when(deviceService.toSummaryResponse(device)).thenReturn(deviceSummary);

        // when
        IncidentResponse result = underTest.toResponse(incident, true);

        // then
        assertThat(result.getAssignments()).hasSize(1);
        assertThat(result.getAssignments().get(0).getTechnician().getFullName()).isEqualTo("Carlos Rivera");
    }

    @Test
    @DisplayName("toResponse excludes assignments when includeAssignments is false")
    void should_exclude_assignments_when_include_assignments_false() {
        // given
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder().deviceId(9001L).build();
        when(deviceService.toSummaryResponse(device)).thenReturn(deviceSummary);

        // when
        IncidentResponse result = underTest.toResponse(incident, false);

        // then
        assertThat(result.getAssignments()).isNull();
    }
}
